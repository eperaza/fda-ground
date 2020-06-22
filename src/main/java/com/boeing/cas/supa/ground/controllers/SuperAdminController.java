package com.boeing.cas.supa.ground.controllers;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.NewUser;
import com.boeing.cas.supa.ground.services.AzureADClientService;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.ControllerUtils;

@RequestMapping(path="/superadmin")
@Controller
public class SuperAdminController {

	private final Logger logger = LoggerFactory.getLogger(SuperAdminController.class);

	@Autowired
	private AzureADClientService aadClient;

	@RequestMapping(path="/users", method = { RequestMethod.POST })
	public ResponseEntity<Object> createUser(@RequestBody NewUser newUserPayload, @RequestHeader("Authorization") String authToken) {

		// Extract the access token from the authorization request header
		String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);

		Object result = aadClient.createUser(newUserPayload, accessTokenInRequest, null, newUserPayload.getRoleGroupName(), false);

		if (result instanceof ApiError) {
			ApiError error = (ApiError) result;
			logger.error(error.getErrorLabel(), error.getErrorDescription());
			return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(error.getFailureReason()));
		}

		return new ResponseEntity<>(result, HttpStatus.CREATED);
	}

	@RequestMapping(path="/createnewusers", method = { RequestMethod.POST })
	public ResponseEntity<Object> createNewUser(@RequestBody NewUser newUserPayload, @RequestHeader("Authorization") String authToken) {

		// Extract the access token from the authorization request header
		String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);

		Object result;

		if(newUserPayload.getAirlineGroupName().trim().equalsIgnoreCase("airline-amx")){
			 result = aadClient.createUser(newUserPayload, accessTokenInRequest, null, newUserPayload.getRoleGroupName(), false);
		}else{
			result = aadClient.createUser(newUserPayload, accessTokenInRequest, null, newUserPayload.getRoleGroupName(), true);
		}

		if (result instanceof ApiError) {
			ApiError error = (ApiError) result;
			logger.error(error.getErrorLabel(), error.getErrorDescription());
			return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(error.getFailureReason()));
		}

		return new ResponseEntity<>(result, HttpStatus.CREATED);
	}

	@RequestMapping(path="/users/{userId}", method = { RequestMethod.DELETE })
	public ResponseEntity<Object> deleteUser(@PathVariable("userId") String userId, @RequestHeader("Authorization") String authToken) {

		// Extract the access token from the authorization request header
		String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);

		// Delete user based on supplied user object ID.
		Object result = aadClient.deleteUser(userId, accessTokenInRequest, true);

		if (result instanceof ApiError) {
			return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
		}

		return new ResponseEntity<>(result, HttpStatus.NO_CONTENT);
	}
}
