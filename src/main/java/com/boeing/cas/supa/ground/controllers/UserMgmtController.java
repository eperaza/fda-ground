package com.boeing.cas.supa.ground.controllers;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.NewUser;
import com.boeing.cas.supa.ground.services.AzureADClientService;
import com.boeing.cas.supa.ground.services.UserMgmtService;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.ControllerUtils;
import com.boeing.cas.supa.ground.utils.OldEmailRegistrations;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.boeing.cas.supa.ground.exceptions.FileDownloadException;
import com.boeing.cas.supa.ground.exceptions.TspConfigLogException;
import com.boeing.cas.supa.ground.exceptions.UserAccountRegistrationException;
import com.boeing.cas.supa.ground.pojos.*;

@RequestMapping(path = "/userMgmt")
@Controller
public class UserMgmtController {

	private final Logger logger = LoggerFactory.getLogger(UserMgmtController.class);

	@Autowired
	private AzureADClientService aadClient;

	@Autowired
	private UserMgmtService umClient;

	@Autowired
	public OldEmailRegistrations oldEmailList;

	@RequestMapping(path = "/deleteUser/{userId}", method = { RequestMethod.DELETE })
	public ResponseEntity<Object> deleteUserByAirline(@PathVariable("userId") String userId,
			@RequestHeader("Authorization") String authToken, @RequestHeader("Membership") String membership,
			@RequestHeader("Role") String role) {

		// Extract the access token from the authorization request header
		String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);

		// Delete user based on supplied user object ID.
		Object result = umClient.deleteUser(userId, accessTokenInRequest, true, membership, role);

		if (result instanceof ApiError) {
			return new ResponseEntity<>(result, ControllerUtils
					.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
		}

		return new ResponseEntity<>(result, HttpStatus.NO_CONTENT);
	}

	@RequestMapping(path = "/getUsers", method = { RequestMethod.GET })
	public ResponseEntity<Object> getUsers(@RequestHeader("Authorization") String authToken,
			@RequestHeader("Airline") String airline) {

		// Extract the access token from the authorization request header
		String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);

		// Create user with the received payload/parameters defining the new account.
		Object result = umClient.getUsers(accessTokenInRequest, airline);

		if (result instanceof ApiError) {
			return new ResponseEntity<>(result, ControllerUtils
					.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
		}

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@RequestMapping(path = "/getPreUsers", method = { RequestMethod.GET })
	public ResponseEntity<Object> getPreUsers(@RequestHeader("Authorization") String authToken,
			@RequestHeader("Airline") String airline) {

		// Create user with the received payload/parameters defining the new account.
		Object result = umClient.getPreUsers(airline);

		if (result instanceof ApiError) {
			return new ResponseEntity<>(result, ControllerUtils
					.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
		}

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@RequestMapping(path = "/createPreUser", method = { RequestMethod.POST })
	public ResponseEntity<Object> createNewUser(@RequestBody PreUserAccount userPayload,
			@RequestHeader("Authorization") String authToken) throws UserAccountRegistrationException {

		Object result;

		PreUserAccount newUserPayload = new PreUserAccount(userPayload.getUserId(), userPayload.getFirst(),
				userPayload.getLast(), userPayload.getEmail(),
				Constants.UserAccountState.PENDING_USER_ACTIVATION.toString(), userPayload.getAirline(),
				userPayload.getRole());

		result = umClient.createUser(newUserPayload);

		if (result instanceof ApiError) {
			ApiError error = (ApiError) result;
			logger.error(error.getErrorLabel(), error.getErrorDescription());
			return new ResponseEntity<>(result,
					ControllerUtils.translateRequestFailureReasonToHttpErrorCode(error.getFailureReason()));
		}

		return new ResponseEntity<>(result, HttpStatus.CREATED);
	}

	@RequestMapping(path = "/updatePreUser", method = { RequestMethod.PUT })
	public ResponseEntity<Object> updatePreUser(@RequestBody PreUserAccount userPayload,
			@RequestHeader("Authorization") String authToken) throws UserAccountRegistrationException {

		Object result;

		PreUserAccount newUserPayload = new PreUserAccount(userPayload.getUserId(), userPayload.getFirst(),
				userPayload.getLast(), userPayload.getEmail(),
				Constants.UserAccountState.PENDING_USER_ACTIVATION.toString(), userPayload.getAirline(),
				userPayload.getRole());

		result = umClient.updatePreUser(newUserPayload);

		if (result instanceof ApiError) {
			ApiError error = (ApiError) result;
			logger.error(error.getErrorLabel(), error.getErrorDescription());
			return new ResponseEntity<>(result,
					ControllerUtils.translateRequestFailureReasonToHttpErrorCode(error.getFailureReason()));
		}

		return new ResponseEntity<>(result, HttpStatus.CREATED);
	}

	@RequestMapping(path = "/deletePreUser/{userId:.+}/", method = { RequestMethod.DELETE })
	public ResponseEntity<Object> deleteUserPreUser(@PathVariable("userId") String userId,
			@RequestHeader("Authorization") String authToken) throws UserAccountRegistrationException {

		// Delete user based on supplied user object ID.
		Object result = umClient.deletePreUser(userId);

		if (result instanceof ApiError) {
			return new ResponseEntity<>(result, ControllerUtils
					.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
		}

		return new ResponseEntity<>(result, HttpStatus.NO_CONTENT);
	}

	@RequestMapping(path = "/createnewusers", method = { RequestMethod.POST })
	public ResponseEntity<Object> createNewUser(@RequestBody NewUser newUserPayload,
			@RequestHeader("Authorization") String authToken) {

		Object preferences = umClient.getAirlinePreferences(newUserPayload.getAirlineGroupName());
		logger.error("preference info--" + preferences.toString());
		boolean isMP = false;
		ObjectMapper mapper = new ObjectMapper();
		try {

			AirlinePreferences[] parser = mapper.readValue(preferences.toString(), AirlinePreferences[].class);
			logger.error("preference info--Parsing done");
			for (AirlinePreferences pref : parser) {
				if (pref.getAirlineKey().contains(Constants.MP_FOR_REGISTRATION) && pref.isEnabled()) {
					logger.debug("preference {} is enabled: {} ", pref.getAirlineKey().toString(), pref.isEnabled());
					isMP = true;
				}
			}

		} catch (Exception e) {
			logger.error(e.getMessage());
			logger.error("prefrence info-- Error Occured");

		}

		Object result;
		if (isMP) {
			logger.debug(" **** OLD AIRLINE REGISTRATION PROCESS **** ");

			result = aadClient.createUser(newUserPayload, authToken, null,
					newUserPayload.getRoleGroupName(), false);
		} else {
			logger.debug(" **** NEW AIRLINE REGISTRATION PROCESS **** ");
			result = aadClient.createUser(newUserPayload, authToken, null,
					newUserPayload.getRoleGroupName(), true);
		}

		if (result instanceof ApiError) {

			ApiError error = (ApiError) result;
			logger.error(error.getErrorLabel(), error.getErrorDescription());
			return new ResponseEntity<>(result,
					ControllerUtils.translateRequestFailureReasonToHttpErrorCode(error.getFailureReason()));
		}

		return new ResponseEntity<>(result, HttpStatus.CREATED);
	}

	@RequestMapping(path = "/getTSP", method = { RequestMethod.GET })
	public ResponseEntity<Object> getTSP(@RequestHeader String airline,
			@RequestHeader("Authorization") String authToken) throws IOException, TspConfigLogException, FileDownloadException {
				Object result = umClient.getTSP(airline);

		return new ResponseEntity<>(result, HttpStatus.CREATED);

	}

}
