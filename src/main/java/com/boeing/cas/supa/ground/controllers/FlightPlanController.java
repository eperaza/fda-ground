package com.boeing.cas.supa.ground.controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.services.FlightPlanService;
import com.boeing.cas.supa.ground.utils.ControllerUtils;

@RestController
public class FlightPlanController {

	@Value("${api.routesync.perfectflights}")
	private String perfectFlightsUri;

	@Autowired
	private FlightPlanService flightPlanService;

	@GetMapping(path = "/flight_objects", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> getAllFlightPlans(
			@RequestParam("flightId") Optional<String> flightId,
			@RequestParam("departureAirport") Optional<String> departureAirport,
			@RequestParam("arrivalAirport") Optional<String> arrivalAirport,
			@RequestHeader("Authorization") String authToken) {

		Object allFlightPlansResponse = this.flightPlanService.getAllFlightPlans(flightId, departureAirport, arrivalAirport, authToken);
		if (allFlightPlansResponse instanceof ApiError) {
			ApiError errorResponse = (ApiError) allFlightPlansResponse;
			return new ResponseEntity<>(errorResponse, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(errorResponse.getFailureReason()));
		}

		return new ResponseEntity<>(allFlightPlansResponse, HttpStatus.OK);
	}

	@GetMapping(path = "/flight_objects/{id}/show", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> getFlightPlanByFlightId(
			@PathVariable("id") String id,
			@RequestHeader("Authorization") String authToken) {

		Object flightPlanResponse = this.flightPlanService.getFlightPlanById(id, authToken);
		if (flightPlanResponse instanceof ApiError) {
			ApiError errorResponse = (ApiError) flightPlanResponse;
			return new ResponseEntity<>(errorResponse, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(errorResponse.getFailureReason()));
		}

		return new ResponseEntity<>(flightPlanResponse, HttpStatus.OK);
	}
}
