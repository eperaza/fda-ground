package com.boeing.cas.supa.ground.controllers;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.services.TspManagementService;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.ControllerUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class TspManagementController {

    @Autowired
    private TspManagementService tspManagementService;

    @RequestMapping(path="/getTspList", method = { RequestMethod.GET })
    public ResponseEntity<Object> getTspList(@RequestHeader("Authorization") String authToken, 
    		@RequestHeader(name = "airline", required = true) String airlineName,
    		@RequestHeader(name = "tail", required = true) String tailNumber) {

        // Extract the access token from the authorization request header
        String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);

        // Create user with the received payload/parameters defining the new account.
        Object result = tspManagementService.getTsps(airlineName, tailNumber);

        if (result instanceof ApiError) {
            return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(path="/getActiveTsp", method = { RequestMethod.GET })
    public ResponseEntity<Object> getActiveTsp(@RequestHeader("Authorization") String authToken, 
    		@RequestHeader(name = "airline", required = true) String airlineName,
    		@RequestHeader(name = "tail", required = true) String tailNumber,
    		@RequestHeader(name = "stage", required = false, defaultValue = "PROD") String stage) {

        // Extract the access token from the authorization request header
        String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);

        // Create user with the received payload/parameters defining the new account.
        Object result = tspManagementService.getActiveTspByAirlineAndTailNumberAndStage(airlineName, tailNumber, stage);

        if (result instanceof ApiError) {
            return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(path="/updateTsp", method = { RequestMethod.POST })
    public ResponseEntity<Object> updateTsp(@RequestBody String tspContent, 
    		@RequestHeader("Authorization") String authToken,
    		@RequestHeader(name = "airline", required = true) String airlineName,
    		@RequestHeader(name = "stage", required = false, defaultValue = "TEST") String stage,
    		@RequestHeader(name = "effectiveDate", required = false) String effectiveDate) {

        // Extract the access token from the authorization request header
        String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);
        String userId = "TEST"; // TODO: Get it from the token;

        // Create user with the received payload/parameters defining the new account.
        Object result = tspManagementService.saveTsp(airlineName, tspContent, stage, effectiveDate, userId);

        if (result instanceof ApiError) {
            return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
