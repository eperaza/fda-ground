package com.boeing.cas.supa.ground.controllers;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.KeyList;
import com.boeing.cas.supa.ground.pojos.KeyValueUpdate;
import com.boeing.cas.supa.ground.services.AzureADClientService;
import com.boeing.cas.supa.ground.services.FeatureManagementService;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.ControllerUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import java.util.Map;
import java.util.List;



@Controller
public class FeatureManagementController {

    private final Logger logger = LoggerFactory.getLogger(FeatureManagementController.class);

    @Autowired
    private Map<String, String> appProps;

    @Autowired
    private AzureADClientService aadClient;

    @Autowired
    private FeatureManagementService featureManagementService;
   
 

    @RequestMapping(path="/retrieveZuppa", method = { RequestMethod.GET })
    public ResponseEntity<Object> retrieveZuppa(@RequestHeader("Authorization") String authToken, String airline) {
        // Extract the access token from the authorization request header
        String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);
        Object result = featureManagementService.getAppSecrets(accessTokenInRequest,airline);
        logger.debug("from service-->>"+result.toString());
        if (result instanceof ApiError) {
            return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
    @RequestMapping(path="/featuremanagement", method = { RequestMethod.GET })
    public ResponseEntity<Object> getFeatureManagement(@RequestHeader("Authorization") String authToken) {

        // Extract the access token from the authorization request header
        String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);


        // Create user with the received payload/parameters defining the new account.
        Object result = featureManagementService.getFeatureManagement(accessTokenInRequest);

        if (result instanceof ApiError) {
            return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }


    @RequestMapping(path="/updateFeatureManagement", method = { RequestMethod.POST })
    public ResponseEntity<Object> updateFeatureManagement(@RequestBody KeyList keysToUpdate, @RequestHeader("Authorization") String authToken) {

        // Extract the access token from the authorization request header
        String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);

        // Create user with the received payload/parameters defining the new account.
        Object result = featureManagementService.updateFeatureManagement(accessTokenInRequest, keysToUpdate);

        if (result instanceof ApiError) {
            return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(path="/updateAirlinePreferences", method = { RequestMethod.POST })
    public ResponseEntity<Object> updateAirlinePreferences(@RequestBody KeyList keysToUpdate, @RequestHeader("Authorization") String authToken) {

        // Extract the access token from the authorization request header
        String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);


        // Create user with the received payload/parameters defining the new account.
        Object result = featureManagementService.updateAirlinePreferences(accessTokenInRequest, keysToUpdate);

        if (result instanceof ApiError) {
            return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(path="/updateUserPreferences", method = { RequestMethod.POST })
    public ResponseEntity<Object> updateUserPreferences(@RequestBody  KeyList keysToUpdate, @RequestHeader("Authorization") String authToken) {

        // Extract the access token from the authorization request header
        String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);


        // Create user with the received payload/parameters defining the new account.
        Object result = featureManagementService.updateUserPreferences(accessTokenInRequest, keysToUpdate);

        if (result instanceof ApiError) {
            return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(path="/airlinepreferences", method = { RequestMethod.GET })
    public ResponseEntity<Object> getAirlinePreferences(@RequestHeader("Authorization") String authToken) {

        // Extract the access token from the authorization request header
        String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);


        // Create user with the received payload/parameters defining the new account.
        Object result = featureManagementService.getAirlinePreferences(accessTokenInRequest);

        if (result instanceof ApiError) {
            return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(path="/userpreferences", method = { RequestMethod.GET })
    public ResponseEntity<Object> getUserPreferences(@RequestHeader("Authorization") String authToken) {

        // Extract the access token from the authorization request header
        String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);


        // Create user with the received payload/parameters defining the new account.
        Object result = featureManagementService.getUserPreferences(accessTokenInRequest);

        if (result instanceof ApiError) {
            return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
