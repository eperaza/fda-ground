package com.boeing.cas.supa.ground.controllers;

import com.boeing.cas.supa.ground.exceptions.FileDownloadException;
import com.boeing.cas.supa.ground.pojos.*;
import com.boeing.cas.supa.ground.services.AircraftPropertyService;
import com.boeing.cas.supa.ground.services.AzureADClientService;
import com.boeing.cas.supa.ground.services.FileManagementService;
import com.boeing.cas.supa.ground.services.FeatureManagementService;
import com.boeing.cas.supa.ground.services.UploadService;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.ControllerUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequestMapping(path="/airlinefocaladmin")
@Controller
public class AirlineFocalAdminController {

	private final Logger logger = LoggerFactory.getLogger(AirlineFocalAdminController.class);

	@Autowired
	private UploadService uploadService;
	
	@Autowired
    private FeatureManagementService featureManagementService;

	private static final List<String> ALLOWED_USER_ROLES = Arrays.asList(
		new String[] { "role-airlinefocal", "role-airlinepilot", "role-airlinemaintenance", "role-airlinecheckairman", "role-airlineefbadmin" });

	@Autowired
	private AzureADClientService aadClient;
	
	@Autowired
	private FileManagementService fileManagementService;
	
	  @Autowired
	    private AircraftPropertyService aircraftPropertyService;

	public AirlineFocalAdminController(UploadService uploadService) {
		this.uploadService = uploadService;
	}
	 @RequestMapping(path="/getFlightStatus", method = { RequestMethod.GET })
	    public ResponseEntity<Object> getFlightStatus(@RequestHeader("Authorization") String authToken, 
	    		@RequestHeader(name = "airline", required = true) String airline) {

	        logger.debug("File Status for airline: " + airline);
	      

	        Date result = aircraftPropertyService.getLastModifedAirline(authToken, airline);
	        	try {
					Date result2=fileManagementService.getLastModifedDateForContainer(authToken,airline);
					if(result==result2)
					{
								return new ResponseEntity<>(result.toString() +"--matched--"+result2.toString(), HttpStatus.OK);
					}
					else
					{
						return new ResponseEntity<>(result.toString() +"--not matched--"+result2.toString(), HttpStatus.OK);
					}
				} catch (FileDownloadException e) {
					return new ResponseEntity<>(result.toString() +"--not matched--", HttpStatus.OK);
					// TODO Auto-generated catch block
				//	e.printStackTrace();
				} catch (IOException e) {
					return new ResponseEntity<>(result.toString() +"--not matched--", HttpStatus.OK);
					// TODO Auto-generated catch block
				//	e.printStackTrace();
				}

	       // return new ResponseEntity<>("No Match", HttpStatus.INTERNAL_SERVER_ERROR);
	    }

	@RequestMapping(path="/users", method = { RequestMethod.POST })
	public ResponseEntity<Object> createUser(@RequestBody NewUser newUserPayload, @RequestHeader("Authorization") String authToken) {

		// Extract the access token from the authorization request header
		String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);
		List<AirlinePreferences> prefList= new ArrayList<>();



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
				newUserPayload.getRoleGroupName(), false);
		if (result instanceof ApiError) {
			return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(result, HttpStatus.CREATED);
	}

	@RequestMapping(path="/registerusersbulk", method = {RequestMethod.POST })
	public ResponseEntity<Object> createMultipleUsers(@RequestParam("file") MultipartFile file, @RequestHeader("Authorization") String authToken) throws Exception{

		// Extract the access token from the authorization request header
		String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);

		// Get group membership of user issuing request. Ensure that user belongs to role-airlinefocal group
		// and one and only one airline group.
		User airlineFocalCurrentUser = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);
		List<Group> airlineGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());

		List<Map<String, String>> result = new ArrayList<>();
		logger.debug("*** EXCEL FILE RECEIVED for Bulk Registration");

		// default role - pilot
		String defaultRole = ALLOWED_USER_ROLES.get(1);

		result = uploadService.upload(file);

		logger.debug("!! Excel OK - trying to register: ");
		for(Map<String, String> person : result){
			logger.debug(person.toString());
		}

		for(Map<String, String> user : result){
			final ObjectMapper mapper  = new ObjectMapper();
			final UserFromExcel excelUser = mapper.convertValue(user, UserFromExcel.class);

			NewUser userFromExcel = new NewUser(
					excelUser.username,
					excelUser.first_name,
					excelUser.last_name,
					excelUser.password,
					excelUser.email,
					airlineGroups.get(0),
					defaultRole
			);

			if(airlineGroups.get(0).getDisplayName().equalsIgnoreCase("airline-amx")){
				logger.debug("Old REG Hit");
				aadClient.createUser(userFromExcel, authToken, airlineGroups.get(0), userFromExcel.getRoleGroupName(), false);
			}else{
				logger.debug("New Reg Hit");
				aadClient.createUser(userFromExcel, authToken, airlineGroups.get(0), userFromExcel.getRoleGroupName(), true);
			}
		}
		return new ResponseEntity<>(result, HttpStatus.CREATED);
	}

	@RequestMapping(path="/createnewusers", method = { RequestMethod.POST })
	public ResponseEntity<Object> createNewUser(@RequestBody NewUser newUserPayload, @RequestHeader("Authorization") String authToken) {

		// Extract the access token from the authorization request header
		String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);

		// Get group membership of user issuing request. Ensure that user belongs to role-airlinefocal group
		// and one and only one airline group.
		User airlineFocalCurrentUser = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);

		// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
		List<Group> airlineGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());
		// Create user with the received payload/parameters defining the new account.
		List<AirlinePreferences> prefList= new ArrayList<>();
		
				Object preferences = featureManagementService.getAirlinePreferences(accessTokenInRequest);
		logger.error("preference info--"+preferences.toString());
		boolean isMP=false;
		ObjectMapper mapper = new ObjectMapper();
		try {
			
			AirlinePreferences[]  parser = mapper.readValue(preferences.toString(), AirlinePreferences[].class);
		    logger.error("preference info--Parsing done");
			for(AirlinePreferences pref:parser)
			{
				if(pref.getAirlineKey().contains(Constants.MP_FOR_REGISTRATION) && pref.isEnabled()){
					logger.debug("preference {} is enabled: {} ", pref.getAirlineKey().toString(), pref.isEnabled());
					isMP=true;
				}
			}
		
		} catch (Exception e) {
			logger.error(e.getMessage());
			logger.error("prefrence info-- Error Occured");
		
		}		
		List<Group> roleGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_USER_ROLE_PREFIX)).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());
		if (airlineGroups.size() != 1 || roleGroups.size() != 1) {
			return new ResponseEntity<>(new ApiError("MISSING_OR_INVALID_MEMBERSHIP", "User membership is ambiguous, within airlines or roles"), HttpStatus.UNAUTHORIZED);
		}

		Object result;
		if(isMP){
			logger.debug(" **** OLD AIRLINE REGISTRATION PROCESS **** ");

			result = aadClient.createUser(newUserPayload, accessTokenInRequest, airlineGroups.get(0), newUserPayload.getRoleGroupName(), false);
		}else{
			logger.debug(" **** NEW AIRLINE REGISTRATION PROCESS **** ");
			result = aadClient.createUser(newUserPayload, accessTokenInRequest, airlineGroups.get(0), newUserPayload.getRoleGroupName(), true);
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

		// Delete user based on supplied user obect ID.
		Object result = aadClient.deleteUser(userId, accessTokenInRequest, false);

		if (result instanceof ApiError) {
			return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
		}

		return new ResponseEntity<>(result, HttpStatus.NO_CONTENT);
	}

	@RequestMapping(path="/users", method = { RequestMethod.GET })
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

	@RequestMapping(path="/loadusers", method = { RequestMethod.GET })
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
