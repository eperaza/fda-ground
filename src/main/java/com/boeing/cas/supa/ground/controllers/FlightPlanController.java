package com.boeing.cas.supa.ground.controllers;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.services.FlightObjectManagerService;
import com.boeing.cas.supa.ground.utils.ControllerUtils;

@RestController
public class FlightPlanController {

	private final Logger logger = LoggerFactory.getLogger(FlightPlanController.class);

	@Autowired
	private FlightObjectManagerService flightObjectManagerService;

	@GetMapping(path = "/flight_objects", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> getAllFlightObjects(
			@RequestParam("flightId") Optional<String> flightId,
			@RequestParam("departureAirport") Optional<String> departureAirport,
			@RequestParam("arrivalAirport") Optional<String> arrivalAirport,
			@RequestHeader("Authorization") String authToken) {

		logger.debug("Get all flight_objects request");

		Object allFlightObjectsResponse = this.flightObjectManagerService.getAllFlightObjects(flightId, departureAirport, arrivalAirport, authToken);
		if (allFlightObjectsResponse instanceof ApiError) {
			ApiError errorResponse = (ApiError) allFlightObjectsResponse;
			return new ResponseEntity<>(errorResponse, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(errorResponse.getFailureReason()));
		}

		return new ResponseEntity<>(allFlightObjectsResponse, HttpStatus.OK);
	}

	@GetMapping(path = "/flight_objects/{id}/show", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> getFlightObjectById(
			@PathVariable("id") String id,
			@RequestHeader("Authorization") String authToken) {

		logger.debug("Get flight_object request for id:{}", id);
		Object flightObjectResponse = this.flightObjectManagerService.getFlightObjectById(id, authToken);
		if (flightObjectResponse instanceof ApiError) {
			ApiError errorResponse = (ApiError) flightObjectResponse;
			return new ResponseEntity<>(errorResponse, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(errorResponse.getFailureReason()));
		}

		return new ResponseEntity<>(flightObjectResponse, HttpStatus.OK);
	}

	@GetMapping(path = "/operational_flight_plans/{flightPlanId}/show", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> getOperationalFlightPlanByFlightPlanId(
			@PathVariable("flightPlanId") String flightPlanId,
			@RequestHeader("Authorization") String authToken) {

		logger.debug("Get operational_flight_plans request for flightPlanId:{}", flightPlanId);

		Object operationalflightPlanResponse = this.flightObjectManagerService.getOperationalFlightPlanByFlightPlanId(flightPlanId, authToken);
		if (operationalflightPlanResponse instanceof ApiError) {
			ApiError errorResponse = (ApiError) operationalflightPlanResponse;
			return new ResponseEntity<>(errorResponse, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(errorResponse.getFailureReason()));
		}

		return new ResponseEntity<>(operationalflightPlanResponse, HttpStatus.OK);
	}
}
