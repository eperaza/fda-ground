package com.boeing.cas.supa.ground.controllers;

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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boeing.cas.supa.ground.pojos.ApiError;

@RestController
public class FlightPlanController {

	private final Logger logger = LoggerFactory.getLogger(FlightPlanController.class);

	@Value("${api.routesync.perfectflights}")
	private String perfectFlightsUri;

	@Autowired
	private SSLSocketFactory sslSocketFactory;

	@GetMapping(path = "/perfect_flights", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> getAllFlightPlans(@RequestParam("flightId") Optional<String> flightId,
			@RequestParam("departureAirport") Optional<String> departureAirport,
			@RequestParam("arrivalAirport") Optional<String> arrivalAirport,
			@RequestHeader("Authorization") String authToken) {

		try {

			StringBuilder pfUrl = new StringBuilder(perfectFlightsUri);
			if (flightId.isPresent() || departureAirport.isPresent() || arrivalAirport.isPresent()) {
				pfUrl.append('?');

				if (flightId.isPresent() && pfUrl.toString().charAt(pfUrl.length()-1) != '?') {
					pfUrl.append('&').append("flightId=").append(flightId.get());
				} else if (flightId.isPresent()) {
					pfUrl.append("flightId=").append(flightId.get());
				} else {
					// parameter not passed
				}

				if (departureAirport.isPresent() && pfUrl.toString().charAt(pfUrl.length()-1) != '?') {
					pfUrl.append('&').append("departureAirport=").append(departureAirport.get());
				} else if (departureAirport.isPresent()) {
					pfUrl.append("departureAirport=").append(departureAirport.get());
				} else {
					// parameter not passed
				}

				if (arrivalAirport.isPresent() && pfUrl.toString().charAt(pfUrl.length()-1) != '?') {
					pfUrl.append('&').append("arrivalAirport=").append(arrivalAirport.get());
				} else if (arrivalAirport.isPresent()) {
					pfUrl.append("arrivalAirport=").append(arrivalAirport.get());
				} else {
					// parameter not passed
				}
			}
            URL url = new URL(pfUrl.toString());
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            logger.debug("Opened secure HTTPS connection");
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setUseCaches(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            StringBuilder stringBuilder = new StringBuilder();
            int responseCode = connection.getResponseCode();
            logger.debug("HTTP response code = {}", responseCode);
            try (BufferedReader reader = responseCode == HttpStatus.OK.value()
                    ? new BufferedReader(new InputStreamReader(connection.getInputStream()))
                    : new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
            }

			return new ResponseEntity<>(stringBuilder.toString(), HttpStatus.OK);
		} catch (Exception ex) {
			logger.error("Request flight plan failed: {}", ex.getMessage(), ex);
			return new ResponseEntity<>(new ApiError("FLIGHT_PLAN_REQUEST", ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
