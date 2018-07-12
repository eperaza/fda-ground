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
public class FlightPlanService {

	private final Logger logger = LoggerFactory.getLogger(FlightPlanService.class);

	@Value("${api.routesync.perfectflights}")
	private String perfectFlightsUri;

	@Autowired
	private SSLSocketFactory sslSocketFactory;

	public Object getAllFlightPlans(Optional<String> flightId,
			Optional<String> departureAirport,
			Optional<String> arrivalAirport,
			String authToken) {

		try {

			StringBuilder pfUrl = new StringBuilder(perfectFlightsUri);
			if (flightId.isPresent() || departureAirport.isPresent() || arrivalAirport.isPresent()) {
				pfUrl.append('?');

				if (flightId.isPresent() && pfUrl.toString().charAt(pfUrl.length() - 1) != '?') {
					pfUrl.append('&').append("flightId=").append(flightId.get());
				} else if (flightId.isPresent()) {
					pfUrl.append("flightId=").append(flightId.get());
				} else {
					// parameter not passed
				}

				if (departureAirport.isPresent() && pfUrl.toString().charAt(pfUrl.length() - 1) != '?') {
					pfUrl.append('&').append("departureAirport=").append(departureAirport.get());
				} else if (departureAirport.isPresent()) {
					pfUrl.append("departureAirport=").append(departureAirport.get());
				} else {
					// parameter not passed
				}

				if (arrivalAirport.isPresent() && pfUrl.toString().charAt(pfUrl.length() - 1) != '?') {
					pfUrl.append('&').append("arrivalAirport=").append(arrivalAirport.get());
				} else if (arrivalAirport.isPresent()) {
					pfUrl.append("arrivalAirport=").append(arrivalAirport.get());
				} else {
					// parameter not passed
				}
			}

			URL url = new URL(pfUrl.toString());
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

			return stringBuilder.toString();
		} catch (Exception ex) {
			logger.error("Request flight plan failed: {}", ex.getMessage(), ex);
			return new ApiError("FLIGHT_PLANS_REQUEST", ex.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR);
		}
	}

	public Object getFlightPlanById(String id, String authToken) {

		try {

			StringBuilder pfUrl = new StringBuilder(perfectFlightsUri);
			pfUrl.append('/').append(id).append("/show");

			URL url = new URL(pfUrl.toString());
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
				throw new Exception("Flight Plan Service returned " + responseCode);
			}

			return stringBuilder.toString();
		} catch (Exception ex) {
			logger.error("Request flight plan failed: {}", ex.getMessage(), ex);
			return new ApiError("FLIGHT_PLANS_REQUEST", ex.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR);
		}
	}
}
