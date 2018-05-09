package com.boeing.cas.supa.ground.controllers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

import com.boeing.cas.supa.ground.pojos.Error;
import com.boeing.cas.supa.ground.pojos.Group;
import com.boeing.cas.supa.ground.pojos.NewUser;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.services.AzureADClientService;
import com.boeing.cas.supa.ground.utils.Constants;

@Controller
public class AirlineFocalAdminController {

	private final Logger logger = LoggerFactory.getLogger(AirlineFocalAdminController.class);

	private static final List<String> ALLOWED_USER_ROLES = Arrays.asList(new String[] { "role-airlinepilot", "role-airlinemaintenance" });
	
	@Autowired
	private AzureADClientService aadClient;

	@RequestMapping(path="/airlinefocaladmin/users", method = { RequestMethod.POST })
	public ResponseEntity<Object> createUser(@RequestBody NewUser newUserPayload, @RequestHeader("Authorization") String authToken) {

		// Extract the access token from the authorization request header
		String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);
		
		 // Get group membership of user issuing request. Ensure that user belongs to role-airlinefocal group
		 // and one and only one airline group.
		User airlineFocalCurrentUser = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);
		// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
		List<Group> airlineGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith("airline-")).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());
		List<Group> roleGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().equals("role-airlinefocal")).peek(g -> logger.info("Role Group: {}", g)).collect(Collectors.toList());
		if (airlineGroups.size() != 1 || roleGroups.size() != 1) {
			return new ResponseEntity<>(new Error("MISSING_OR_INVALID_MEMBERSHIP", "User membership is ambiguous"), HttpStatus.UNAUTHORIZED);
		}

		// Validate role-based group requested for new user - must be either role-airlinepilot or role-airlinemaintenance
		if (!ALLOWED_USER_ROLES.contains(newUserPayload.getRoleGroupName())) {
			return new ResponseEntity<>(new Error("CREATE_USER_FAILURE", "Missing or invalid user role requested"), HttpStatus.BAD_REQUEST);
		}
		
		// Create user with the received payload/parameters defining the new account.
		Object result = aadClient.createUser(
				newUserPayload,
				accessTokenInRequest,
				airlineGroups.get(0),
				newUserPayload.getRoleGroupName());
		if (result instanceof Error) {
			return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(result, HttpStatus.CREATED);
	}
	
	@RequestMapping(path="/airlinefocaladmin/users/{userId}", method = { RequestMethod.PATCH })
	public ResponseEntity<Object> enableUserAndSetPassword(@PathVariable String userId, @RequestBody String password, @RequestHeader("Authorization") String authToken) {

		// Extract the access token from the authorization request header
		String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);
		
		 // Get group membership of user issuing request. Ensure that user belongs to role-airlinefocal group
		 // and one and only one airline group.
		User airlineFocalCurrentUser = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);
		if (airlineFocalCurrentUser.getObjectId().equals(userId)) {
			return new ResponseEntity<>(new Error("MISSING_OR_INVALID_MEMBERSHIP", "Cannot update self"), HttpStatus.UNAUTHORIZED);
		}

		// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
		List<Group> airlineGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith("airline-")).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());
		List<Group> roleGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().equals("role-airlinefocal")).peek(g -> logger.info("Role Group: {}", g)).collect(Collectors.toList());
		if (airlineGroups.size() != 1 || roleGroups.size() != 1) {
			return new ResponseEntity<>(new Error("MISSING_OR_INVALID_MEMBERSHIP", "User membership is ambiguous"), HttpStatus.UNAUTHORIZED);
		}

		Object result = aadClient.enableUserAndSetPassword(accessTokenInRequest, userId, password);

		if (result instanceof Error) {
			return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
	}
}
