package com.boeing.cas.supa.ground.services;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import com.boeing.cas.supa.ground.pojos.CosmosDbFlightPlanSource;
import com.boeing.cas.supa.ground.pojos.Group;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.boeing.cas.supa.ground.utils.Constants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

@Service
public class FlightObjectManagerService {

	private final Logger logger = LoggerFactory.getLogger(FlightObjectManagerService.class);

	private static final String FLIGHT_OBJECTS_PATH = "/perfect_flights";
	private static final String OPERATIONAL_FLIGHT_PLANS_PATH = "/flight_plans";

	private static final String FLIGHT_PLAN_CONTAINER = "flight-plan-source";

	@Autowired
	private Map<String, String> appProps;

	@Autowired
	private AzureADClientService aadClient;

	@Autowired
	private SSLSocketFactory sslSocketFactory;

	@Autowired
	private MongoFlightManagerService mongoFlightManagerService;


	public Object getAllFlightObjects(Optional<String> flightId, Optional<String> departureAirport,
			Optional<String> arrivalAirport, String authToken, Optional<Integer> limit)
	{
			final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
			List<Group> airlineGroups = user.getGroups().stream()
				.filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
			if (airlineGroups.size() != 1) {
				return new ApiError("FILE_DOWNLOAD_FAILURE", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED);
			}
			String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);
			CosmosDbFlightPlanSource source = this.getFlightPlanSourceFromBlob(airlineGroup);
			logger.debug("getting the airline group "+airlineGroup);
			logger.debug("getting the airline source "+source);
			logger.debug("getting the airline flightId "+flightId);
			logger.debug("getting the airline departureAirport "+departureAirport);
			logger.debug("getting the airline arrivalAirport "+arrivalAirport);
			logger.debug("getting the query limit "+limit);

			// if the CosmosDB flight plan is available, use it - otherwise use RS.
			if (source != null ) {
				logger.debug("Use CosmosDb to obtain the flight plan.");
				return mongoFlightManagerService.getAllFlightObjectsFromCosmosDB(flightId, departureAirport, arrivalAirport, source, limit);
			}
			else if (airlineGroup.equalsIgnoreCase("amx")
					|| airlineGroup.equalsIgnoreCase("fda")
					|| airlineGroup.equalsIgnoreCase("bgs")
					|| airlineGroup.equalsIgnoreCase("tav")) {
				logger.debug("Use [AMX] Route Sync to obtain the flight plan.");
				int counter = 1;
				Object obj = getAllFlightObjectsFromRS(flightId, departureAirport, arrivalAirport, authToken);
				while (obj instanceof ApiError && counter < 5) {
					logger.debug("RS Flight Plans Request FAILED. try Request again {}", counter);
					counter++;
					obj = getAllFlightObjectsFromRS(flightId, departureAirport, arrivalAirport, authToken);
				}
				return obj;
			} else {
				String message = "No flight plan available.";
				logger.debug(message);
				return new ApiError("FLIGHT_OBJECTS_REQUEST", message, RequestFailureReason.NOT_FOUND);
			}
	}




	private Object getAllFlightObjectsFromRS(Optional<String> flightId, Optional<String> departureAirport,
			Optional<String> arrivalAirport, String authToken)
	{

		try {

			StringBuilder fomUrl = new StringBuilder(appProps.get("RouteSyncFOMUrl")).append(FLIGHT_OBJECTS_PATH);
			if (flightId.isPresent() || departureAirport.isPresent() || arrivalAirport.isPresent()) {
				fomUrl.append('?');

				if (flightId.isPresent() && fomUrl.toString().charAt(fomUrl.length() - 1) != '?') {
					fomUrl.append('&').append("flightId=").append(flightId.get());
				} else if (flightId.isPresent()) {
					fomUrl.append("flightId=").append(flightId.get());
				} else {
					// parameter not passed
				}

				if (departureAirport.isPresent() && fomUrl.toString().charAt(fomUrl.length() - 1) != '?') {
					fomUrl.append('&').append("departureAirport=").append(departureAirport.get());
				} else if (departureAirport.isPresent()) {
					fomUrl.append("departureAirport=").append(departureAirport.get());
				} else {
					// parameter not passed
				}

				if (arrivalAirport.isPresent() && fomUrl.toString().charAt(fomUrl.length() - 1) != '?') {
					fomUrl.append('&').append("arrivalAirport=").append(arrivalAirport.get());
				} else if (arrivalAirport.isPresent()) {
					fomUrl.append("arrivalAirport=").append(arrivalAirport.get());
				} else {
					// parameter not passed
				}
			}
			logger.debug("Route Sych URL = [{}]", fomUrl.toString());
			URL url = new URL(fomUrl.toString());
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setSSLSocketFactory(sslSocketFactory);
			connection.setUseCaches(false);
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			StringBuilder stringBuilder = new StringBuilder();
			int responseCode = connection.getResponseCode();
			logger.debug("HTTP response code = {}", responseCode);
			try (BufferedReader reader = (responseCode == HttpStatus.OK.value())
					? new BufferedReader(new InputStreamReader(connection.getInputStream()))
					: new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					stringBuilder.append(line);
				}
			}

			if (responseCode != HttpStatus.OK.value()) {
				throw new Exception("Flight Object Manager service returned " + responseCode);
			}

			return stringBuilder.toString();
		} catch (Exception ex) {
			logger.error("Request all flight objects from RS failed: {}", ex.getMessage(), ex);
			return new ApiError("FLIGHT_OBJECTS_REQUEST", ex.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR);
		}
	}


	public Object getFlightObjectById(String id, String authToken) {

		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream()
				.filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			return new ApiError("FILE_DOWNLOAD_FAILURE", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED);
		}
		String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);

		CosmosDbFlightPlanSource source = this.getFlightPlanSourceFromBlob(airlineGroup);

		// if the CosmosDB flight plan is available, use it - otherwise use RS.
		if (source != null ) {
			logger.debug("Use CosmosDb to obtain the flight plan.");
			return mongoFlightManagerService.getFlightObjectByIdFromCosmosDB(id, source);
		}
		else if (airlineGroup.equalsIgnoreCase("amx")
				|| airlineGroup.equalsIgnoreCase("fda")
				|| airlineGroup.equalsIgnoreCase("bgs")){
			logger.debug("Use [AMX] Route Sync to obtain the flight plan.");
			int counter = 1;
			Object obj = getFlightObjectByIdFromRS(id, authToken);
			while (obj instanceof ApiError && counter < 5) {
				logger.debug("RS Flight Object Request FAILED. try Request again {}", counter);
				counter++;
				obj = getFlightObjectByIdFromRS(id, authToken);
			}
			return obj;
		} else {
			String message = "No flight plan available.";
			logger.debug(message);
			return new ApiError("FLIGHT_OBJECTS_REQUEST", message, RequestFailureReason.NOT_FOUND);
		}
	}


	private Object getFlightObjectByIdFromRS(String id, String authToken) {

		try {

			StringBuilder fomUrl = new StringBuilder(appProps.get("RouteSyncFOMUrl")).append(FLIGHT_OBJECTS_PATH);
			fomUrl.append('/').append(id).append("/show");

			URL url = new URL(fomUrl.toString());
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setSSLSocketFactory(sslSocketFactory);
			connection.setUseCaches(false);
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			StringBuilder stringBuilder = new StringBuilder();
			int responseCode = connection.getResponseCode();
			logger.debug("HTTP response code = {}", responseCode);
			try (BufferedReader reader = (responseCode == HttpStatus.OK.value())
					? new BufferedReader(new InputStreamReader(connection.getInputStream()))
					: new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					stringBuilder.append(line);
				}
			}

			if (responseCode != HttpStatus.OK.value()) {
				throw new Exception("Flight Object Manager service returned " + responseCode);
			}

			return stringBuilder.toString();
		} catch (Exception ex) {
			logger.error("Request flight object from RS failed: {}", ex.getMessage(), ex);
			return new ApiError("FLIGHT_PLANS_REQUEST", ex.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR);
		}
	}


	public Object getOperationalFlightPlanByFlightPlanId(String flightPlanId, String authToken) {

		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream()
				.filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			return new ApiError("FILE_DOWNLOAD_FAILURE", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED);
		}
		String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);

		CosmosDbFlightPlanSource source = this.getFlightPlanSourceFromBlob(airlineGroup);

		// if the CosmosDB flight plan is available, use it - otherwise use RS.
		if (source != null ) {
			logger.debug("Use CosmosDb to obtain the flight plan.");
			return mongoFlightManagerService.getOperationalFlightPlanByFlightPlanIdFromCosmosDB(flightPlanId, source);
		}
		else if (airlineGroup.equalsIgnoreCase("amx") || airlineGroup.equalsIgnoreCase("fda")){
			int counter = 1;
			logger.debug("Use [AMX] Route Sync to obtain the flight plan.");
			Object obj = getOperationalFlightPlanByFlightPlanIdFromRS(flightPlanId, authToken);
			while (obj instanceof ApiError && counter < 5) {
				logger.debug("Flight Plan Request FAILED. try Request again {}", counter);
				counter++;
				obj = getOperationalFlightPlanByFlightPlanIdFromRS(flightPlanId, authToken);
			}
			return obj;
		} else {
			String message = "No flight plan available.";
			logger.debug(message);
			return new ApiError("FLIGHT_OBJECTS_REQUEST", message, RequestFailureReason.NOT_FOUND);
		}

	}



	private Object getOperationalFlightPlanByFlightPlanIdFromRS(String flightPlanId, String authToken) {

		try {

			StringBuilder fomUrl = new StringBuilder(appProps.get("RouteSyncFOMUrl")).append(OPERATIONAL_FLIGHT_PLANS_PATH);
			fomUrl.append('/').append(flightPlanId).append("/show");

			URL url = new URL(fomUrl.toString());
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setSSLSocketFactory(sslSocketFactory);
			connection.setUseCaches(false);
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			StringBuilder stringBuilder = new StringBuilder();
			int responseCode = connection.getResponseCode();
			logger.debug("HTTP response code = {}", responseCode);
			try (BufferedReader reader = (responseCode == HttpStatus.OK.value())
					? new BufferedReader(new InputStreamReader(connection.getInputStream()))
					: new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					stringBuilder.append(line);
				}
			}

			if (responseCode != HttpStatus.OK.value()) {
				throw new Exception("Flight Object Manager service returned " + responseCode);
			}

			return stringBuilder.toString();
		} catch (Exception ex) {
			logger.error("Request operational flight plan from RS failed: {}", ex.getMessage(), ex);
			return new ApiError("OPERATIONAL_FLIGHT_PLAN_REQUEST", ex.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR);
		}
	}


	/**
	 * getFlightPlanSourceFromBlob - gets the Flight Plan Source from Blob
	 * @param airline airline
	 * @return contents of flight plan source
	 */
	private CosmosDbFlightPlanSource getFlightPlanSourceFromBlob(String airline) {

		CosmosDbFlightPlanSource flightPlanSource = new CosmosDbFlightPlanSource(airline);

		String fileName = airline + ".source";
		logger.debug("Retrieve data from [" + fileName + "]");
		try {

			AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"), this.appProps.get("StorageKey"));
			try (ByteArrayOutputStream outputStream = asu.downloadFile(FLIGHT_PLAN_CONTAINER, fileName)) {

				flightPlanSource.addFlightPlanSource(outputStream.toString());

			} catch (NullPointerException npe) {
				logger.error("Failed to retrieve flight plan [{}] from [{}]: {}", fileName, FLIGHT_PLAN_CONTAINER, npe.getMessage());
				flightPlanSource = null;
			}
		}
		catch (IOException | org.apache.commons.configuration.ConfigurationException ioe) {
			logger.error("Failed to retrieve flight plan [{}] from [{}]: {}", fileName, FLIGHT_PLAN_CONTAINER, ioe.getMessage());
			flightPlanSource = null;
		}

		return flightPlanSource;
	}

}
