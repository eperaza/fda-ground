package com.boeing.cas.supa.ground.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

@Service
public class FlightObjectManagerService {

	private final Logger logger = LoggerFactory.getLogger(FlightObjectManagerService.class);

	private static final String FLIGHT_OBJECTS_PATH = "/perfect_flights";
	private static final String OPERATIONAL_FLIGHT_PLANS_PATH = "/flight_plans";
	
	@Value("${api.routesync.fom}")
	private String flightObjectManagerUri;

	@Autowired
	private SSLSocketFactory sslSocketFactory;

	public Object getAllFlightObjects(Optional<String> flightId,
			Optional<String> departureAirport,
			Optional<String> arrivalAirport,
			String authToken) {

		try {

			StringBuilder fomUrl = new StringBuilder(flightObjectManagerUri).append(FLIGHT_OBJECTS_PATH);
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
			logger.error("Request all flight objects failed: {}", ex.getMessage(), ex);
			return new ApiError("FLIGHT_OBJECTS_REQUEST", ex.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR);
		}
	}

	public Object getFlightObjectById(String id, String authToken) {

		try {

			StringBuilder fomUrl = new StringBuilder(flightObjectManagerUri).append(FLIGHT_OBJECTS_PATH);
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
			logger.error("Request flight object failed: {}", ex.getMessage(), ex);
			return new ApiError("FLIGHT_PLANS_REQUEST", ex.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR);
		}
	}

	public Object getOperationalFlightPlanByFlightPlanId(String flightPlanId, String authToken) {

		try {

			StringBuilder fomUrl = new StringBuilder(flightObjectManagerUri).append(OPERATIONAL_FLIGHT_PLANS_PATH);
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
			logger.error("Request operational flight plan failed: {}", ex.getMessage(), ex);
			return new ApiError("OPERATIONAL_FLIGHT_PLAN_REQUEST", ex.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR);
		}
	}
}
