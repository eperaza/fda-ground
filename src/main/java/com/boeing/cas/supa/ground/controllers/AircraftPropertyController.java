package com.boeing.cas.supa.ground.controllers;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.services.AircraftPropertyService;
import com.boeing.cas.supa.ground.services.AzureADClientService;
import com.boeing.cas.supa.ground.services.FileManagementService;
import com.boeing.cas.supa.ground.utils.ControllerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;

@Controller
public class AircraftPropertyController {

    @Autowired
    private AircraftPropertyService aircraftPropertyService;
    private final Logger logger = LoggerFactory.getLogger(AircraftPropertyController.class);

    @Autowired
    private AzureADClientService azureADClientService;

    @Autowired
    private FileManagementService fileManagementService;

    @RequestMapping(path="/getAircraftConfiguration", method={RequestMethod.GET})
    public ResponseEntity<Object> getAircraftConfiguration(@RequestHeader("Authorization") String authToken,
                                         @RequestHeader(name = "tail", required = false) String tailNumber) {

        String airlineName =  azureADClientService.validateAndGetAirlineName(authToken);
        // aircraft config is TSP + AircraftProperty
        // TSP is JSON, Aircraft Property PLAINTEXT
        // zip will have 2 files, json and plaintext
        Object result = aircraftPropertyService.getAircraftConfig(authToken);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(path="/getAircraftProperty", method = { RequestMethod.GET })
    public ResponseEntity<Object> getAircraftProperty(@RequestHeader("Authorization") String authToken,
                                                      @RequestHeader(name = "tailNumber", required = true) String tailNumber) throws IOException {

        logger.debug("got to endpoint with tail: " + tailNumber);

        Object result = aircraftPropertyService.getAircraftProperty(authToken, tailNumber);

        logger.debug("aircraft prop: " + result);
        if (result instanceof ApiError) {
            return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}