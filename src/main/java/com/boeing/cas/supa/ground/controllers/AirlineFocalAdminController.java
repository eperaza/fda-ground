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

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.Group;
import com.boeing.cas.supa.ground.pojos.NewUser;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.services.AzureADClientService;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.ControllerUtils;

@Controller
public class AirlineFocalAdminController {

	private final Logger logger = LoggerFactory.getLogger(AirlineFocalAdminController.class);

	private static final List<String> ALLOWED_USER_ROLES = Arrays.asList(
		new String[] { "role-airlinefocal", "role-airlinepilot", "role-airlinemaintenance", "role-airlinecheckairman", "role-airlineefbadmin" });

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
		List<Group> airlineGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());
		//List<Group> roleGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().equals("role-airlinefocal")).peek(g -> logger.info("Role Group: {}", g)).collect(Collectors.toList());
		// Allow anyone who can access this screen to add users.
		List<Group> roleGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_USER_ROLE_PREFIX)).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());
		if (airlineGroups.size() != 1 || roleGroups.size() != 1) {
			return new ResponseEntity<>(new ApiError("MISSING_OR_INVALID_MEMBERSHIP", "User membership is ambiguous, within airlines or roles"), HttpStatus.UNAUTHORIZED);
		}

		// Validate role-based group requested for new user - must be role-airlinefocal, role-airlinepilot or role-airlinemaintenance
		if (!ALLOWED_USER_ROLES.contains(newUserPayload.getRoleGroupName())) {
			return new ResponseEntity<>(new ApiError("CREATE_USER_FAILURE", "Missing or invalid user role requested"), HttpStatus.BAD_REQUEST);
		}

		// Create user with the received payload/parameters defining the new account.
		Object result = aadClient.createUser(
				newUserPayload,
				accessTokenInRequest,
				airlineGroups.get(0),
				newUserPayload.getRoleGroupName());
		if (result instanceof ApiError) {
			return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(result, HttpStatus.CREATED);
	}

	@RequestMapping(path="/airlinefocaladmin/users/{userId}", method = { RequestMethod.DELETE })
	public ResponseEntity<Object> deleteUser(@PathVariable("userId") String userId, @RequestHeader("Authorization") String authToken) {

		// Extract the access token from the authorization request header
		String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);

		// Delete user based on supplied user obect ID.
		Object result = aadClient.deleteUser(userId, accessTokenInRequest, false);

		if (result instanceof ApiError) {
			return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
		}

		return new ResponseEntity<>(result, HttpStatus.NO_CONTENT);
	}

	@RequestMapping(path="/airlinefocaladmin/users", method = { RequestMethod.GET })
	public ResponseEntity<Object> getUsers(@RequestHeader("Authorization") String authToken) {

		// Extract the access token from the authorization request header
		String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);

		// Create user with the received payload/parameters defining the new account.
		Object result = aadClient.getUsers(accessTokenInRequest);

		if (result instanceof ApiError) {
			return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
		}

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@RequestMapping(path="/airlinefocaladmin/loadusers", method = { RequestMethod.GET })
	public ResponseEntity<Object> loadUsers(@RequestHeader("Authorization") String authToken) {

		// Extract the access token from the authorization request header
		String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);

		// Create user with the received payload/parameters defining the new account.
		Object result = aadClient.loadUsers(accessTokenInRequest);

		if (result instanceof ApiError) {
			return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
		}

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

}
