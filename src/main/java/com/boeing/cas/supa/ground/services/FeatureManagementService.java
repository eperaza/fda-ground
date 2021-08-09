package com.boeing.cas.supa.ground.services;

import com.boeing.cas.supa.ground.dao.FeatureManagementDao;
import com.boeing.cas.supa.ground.exceptions.*;
import com.boeing.cas.supa.ground.pojos.*;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.boeing.cas.supa.ground.utils.ControllerUtils;
import com.boeing.cas.supa.ground.utils.KeyVaultRetriever;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.keyvault.models.SecretItem;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretAsyncClient;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;

@Service
public class FeatureManagementService {

	private final Logger logger = LoggerFactory.getLogger(FeatureManagementService.class);

	@Autowired
	private Map<String, String> appProps;

	@Autowired
	private AzureADClientService aadClient;

	@Autowired
	private FeatureManagementDao featureManagementDao;
	
	@Autowired
	private KeyVaultProperties keyVaultProperties;


	public KeyVaultRetriever getKeyVaultRetriever() {
		return new KeyVaultRetriever("https://fda-ground-test-kv.vault.azure.net/", this.keyVaultProperties.getClientId(),
				this.keyVaultProperties.getClientKey());
	}

	
	public Object getAppSecrets(String accessTokenInRequest,String id) {
		KeyVaultRetriever keyVaultRetriever =getKeyVaultRetriever();
		Object resultObj = null;
		Map<String, String> appSecrets = new ConcurrentHashMap<String, String>();
		String secretsVal=keyVaultRetriever.getSecretByKey("zuppa-"+id);
		
		
		ObjectMapper mapper = new ObjectMapper()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		logger.debug(String.valueOf(keyVaultRetriever.getSecretByKey("adwHost")));

		try {
			resultObj = mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL).setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
					.writeValueAsString(secretsVal);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return appSecrets;
	}
	

	public Object getFeatureManagement(String accessTokenInRequest) {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Get FeatureManagement -");

		try {

			// Get group membership of user issuing request. Ensure that user belongs to role-airlinefocal group
			// and one and only one airline group.
			User airlineFocalCurrentUser = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);

			String airline = "airline-unknown";

			// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
			List<Group> airlineGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());
			if (airlineGroups.size() != 1) {
				return new ApiError("FEATURE_MANAGEMENT_FAILED", "User membership is ambiguous, airlines[" + airlineGroups.size() + "]", RequestFailureReason.UNAUTHORIZED);
			} else {
				airline = airlineGroups.get(0).getDisplayName();
			}
			progressLog.append("\nRequesting user belongs to single airline group and airline focal role group");

			List<FeatureManagement> featureManagement = featureManagementDao.getFeatureManagement(airline);

			ObjectMapper mapper = new ObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.setSerializationInclusion(JsonInclude.Include.NON_NULL);

			logger.info("returned [{}] feature management records of Airline Group [{}]", featureManagement.size(), airline);

			resultObj = mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL).setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
					.writeValueAsString(featureManagement);
			progressLog.append("\nObtained members of group in SQL DB");

		} catch (JsonProcessingException jpe) {
			logger.error("JsonProcessingException: {}", jpe.getMessage(), jpe);
			resultObj = new ApiError("FEATURE_MANAGEMENT_FAILED", "Failed to format feature management records.");
		} catch (FeatureManagementException uare) {
			logger.error("FeatureManagementException: {}", uare.getMessage(), uare);
			resultObj = new ApiError("FEATURE_MANAGEMENT_FAILED", "Unable to locate feature management records.");
		} finally {

			if (resultObj instanceof ApiError) {
				logger.error("FDAGndSvcLog> {}", ControllerUtils.sanitizeString(progressLog.toString()));
			}
		}

		return resultObj;
	}


	public Object updateFeatureManagement(String accessTokenInRequest, KeyList keyList) {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Update FeatureManagement Key -");

		try {

			// Get group membership of user issuing request. Ensure that user belongs to role-airlinefocal group
			// and one and only one airline group.
			User airlineFocalCurrentUser = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);

			String airline = "airline-unknown";

			// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
			List<Group> airlineGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());
			if (airlineGroups.size() != 1) {
				return new ApiError("UPDATE_FEATURE_MANAGEMENT_FAILED", "User membership is ambiguous, airlines[" + airlineGroups.size() + "]", RequestFailureReason.UNAUTHORIZED);
			} else {
				airline = airlineGroups.get(0).getDisplayName();
			}
			progressLog.append("\nRequesting user belongs to single airline group and airline focal role group");

			List<KeyValueUpdate> keysToUpdate = keyList.getKeys();
			logger.info("Feature keys to update, size = [{}]", keysToUpdate.size());

			if (keysToUpdate != null) {
				for (KeyValueUpdate keyToUpdate : keysToUpdate) {
					featureManagementDao.updateFeatureManagement(airline, airlineFocalCurrentUser.getUserPrincipalName(), keyToUpdate);
				}
			}
			List<FeatureManagement> featureManagement = featureManagementDao.getFeatureManagement(airline);

			ObjectMapper mapper = new ObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.setSerializationInclusion(JsonInclude.Include.NON_NULL);

			logger.info("returned [{}] feature management records of Airline Group [{}]", featureManagement.size(), airline);

			resultObj = mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL).setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
					.writeValueAsString(featureManagement);
			progressLog.append("\nObtained members of group in SQL DB");

		} catch (JsonProcessingException jpe) {
			logger.error("JsonProcessingException: {}", jpe.getMessage(), jpe);
			resultObj = new ApiError("FEATURE_MANAGEMENT_FAILED", "Failed to format feature management records.");
		} catch (FeatureManagementException uare) {
			logger.error("FeatureManagementException: {}", uare.getMessage(), uare);
			resultObj = new ApiError("FEATURE_MANAGEMENT_FAILED", "Unable to locate feature management records.");
		} finally {

			if (resultObj instanceof ApiError) {
				logger.error("FDAGndSvcLog> {}", ControllerUtils.sanitizeString(progressLog.toString()));
			}
		}

		return resultObj;
	}



	public Object updateAirlinePreferences(String accessTokenInRequest, KeyList keyList) {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Update Airline Preferences Key -");

		try {

			// Get group membership of user issuing request. Ensure that user belongs to role-airlinefocal group
			// and one and only one airline group.
			User airlineCurrentUser = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);

			String airline = "airline-unknown";

			// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
			List<Group> airlineGroups = airlineCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());
			if (airlineGroups.size() != 1) {
				return new ApiError("UPDATE_AIRLINE_PREFERENCES_FAILED", "User membership is ambiguous, airlines[" + airlineGroups.size() + "]", RequestFailureReason.UNAUTHORIZED);
			} else {
				airline = airlineGroups.get(0).getDisplayName();
			}

			List<Group> roleGroups = airlineCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_USER_ROLE_PREFIX)).collect(Collectors.toList());
			if (roleGroups.size() != 1) {
				return new ApiError("UPDATE_AIRLINE_PREFERENCES_FAILED", "Failed to associate user with a role, roles[" + roleGroups.size() + "]", RequestFailureReason.UNAUTHORIZED);
			}
			String userRole = roleGroups.get(0).getDisplayName();

			progressLog.append("\nRequesting user belongs to single airline group and airline focal role group");

			List<FeatureManagement> featureManagements = featureManagementDao.getFeatureManagement(airline);
			boolean isValid = false;
			for (FeatureManagement fm : featureManagements)
			{
				if (fm.getFeatureKey().equals("showConfigAirlinePreferences")) {
					if (userRole.equals("role-airlinefocal")) {
						if (fm.isChoiceFocal()) isValid = true;
					}
					else if (userRole.equals("role-airlinepilot")) {
						if (fm.isChoicePilot()) isValid = true;
					}
					else if (userRole.equals("role-airlinecheckairman")) {
						if (fm.isChoiceCheckAirman()) isValid = true;
					}
					else if (userRole.equals("role-airlinemaintenance")) {
						if (fm.isChoiceMaintenance()) isValid = true;
					}
					else if (userRole.equals("role-airlineefbadmin")) {
						if (fm.isChoiceEfbAdmin()) isValid = true;
					}
				}
			}

			logger.info("fm information: userRole[{}], isValid-> {}", userRole, isValid);
			// Current user does NOT have permission to change this preference.
			if (!isValid) {
				return new ApiError("UPDATE_AIRLINE_PREFERENCES_FAILED", "user role [" + userRole + "] does NOT have permission to update this feature [showConfigAirlinePreferences]", RequestFailureReason.UNAUTHORIZED);
			}

			List<KeyValueUpdate> keysToUpdate = keyList.getKeys();
			logger.info("Airline keys to update, size = [{}]", keysToUpdate.size());

			if (keysToUpdate != null) {
				for (KeyValueUpdate keyToUpdate : keysToUpdate) {
					featureManagementDao.updateAirlinePreferences(airline, airlineCurrentUser.getUserPrincipalName(), keyToUpdate);
				}
			}

			List<AirlinePreferences> airlinePreferences = featureManagementDao.getAirlinePreferences(airline, true);

			ObjectMapper mapper = new ObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.setSerializationInclusion(JsonInclude.Include.NON_NULL);

			logger.info("returned [{}] airline preferences records of Airline Group [{}]", airlinePreferences.size(), airline);

			resultObj = mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL).setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
					.writeValueAsString(airlinePreferences);
			progressLog.append("\nObtained members of group in SQL DB");

		} catch (JsonProcessingException jpe) {
			logger.error("JsonProcessingException: {}", jpe.getMessage(), jpe);
			resultObj = new ApiError("AIRLINE_PREFERENCES_FAILED", "Failed to format airline preferences records.");
		} catch (FeatureManagementException uare) {
			logger.error("FeatureManagementException: {}", uare.getMessage(), uare);
			resultObj = new ApiError("AIRLINE_PREFERENCES_FAILED", "Unable to locate airline preferences records.");
		} finally {

			if (resultObj instanceof ApiError) {
				logger.error("FDAGndSvcLog> {}", ControllerUtils.sanitizeString(progressLog.toString()));
			}
		}

		return resultObj;
	}



	public Object updateUserPreferences(String accessTokenInRequest, KeyList keyList) {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Update user Preferences Key -");

		try {

			// Get group membership of user issuing request. Ensure that user belongs to role-airlinefocal group
			// and one and only one airline group.
			User airlineCurrentUser = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);

			String airline = "airline-unknown";

			// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
			List<Group> airlineGroups = airlineCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());
			if (airlineGroups.size() != 1) {
				return new ApiError("UPDATE_USER_PREFERENCES_FAILED", "User membership is ambiguous, airlines[" + airlineGroups.size() + "]", RequestFailureReason.UNAUTHORIZED);
			} else {
				airline = airlineGroups.get(0).getDisplayName();
			}

			List<Group> roleGroups = airlineCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_USER_ROLE_PREFIX)).collect(Collectors.toList());
			if (roleGroups.size() != 1) {
				return new ApiError("UPDATE_USER_PREFERENCES_FAILED", "Failed to associate user with a role, roles[" + roleGroups.size() + "]", RequestFailureReason.UNAUTHORIZED);
			}
			String userRole = roleGroups.get(0).getDisplayName();

			progressLog.append("\nRequesting user belongs to single airline group and airline focal role group");

			List<FeatureManagement> featureManagements = featureManagementDao.getFeatureManagement(airline);
			boolean isValid = false;
			for (FeatureManagement fm : featureManagements)
			{
				if (fm.getFeatureKey().equals("showConfigUserPreferences")) {
					if (userRole.equals("role-airlinefocal")) {
						if (fm.isChoiceFocal()) isValid = true;
					}
					else if (userRole.equals("role-airlinepilot")) {
						if (fm.isChoicePilot()) isValid = true;
					}
					else if (userRole.equals("role-airlinecheckairman")) {
						if (fm.isChoiceCheckAirman()) isValid = true;
					}
					else if (userRole.equals("role-airlinemaintenance")) {
						if (fm.isChoiceMaintenance()) isValid = true;
					}
					else if (userRole.equals("role-airlineefbadmin")) {
						if (fm.isChoiceEfbAdmin()) isValid = true;
					}
				}
			}
			// Current user does NOT have permission to change this preference.
			if (!isValid) {
				return new ApiError("UPDATE_USER_PREFERENCES_FAILED", "user role [" + userRole + "] does NOT have permission to update this feature [showConfigUserPreferences]", RequestFailureReason.UNAUTHORIZED);
			}

			List<KeyValueUpdate> keysToUpdate = keyList.getKeys();
			logger.info("User keys to update, size = [{}]", keysToUpdate.size());

			if (keysToUpdate != null) {
				for (KeyValueUpdate keyToUpdate : keysToUpdate) {
					featureManagementDao.updateUserPreferences(airline, airlineCurrentUser.getUserPrincipalName(), keyToUpdate);
				}
			}

			List<UserPreferences> userPreferences = featureManagementDao.getUserPreferences(airline);

			ObjectMapper mapper = new ObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.setSerializationInclusion(JsonInclude.Include.NON_NULL);

			logger.info("returned [{}] user preferences records of Airline Group [{}]", userPreferences.size(), airline);

			resultObj = mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL).setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
					.writeValueAsString(userPreferences);
			progressLog.append("\nObtained members of group in SQL DB");

		} catch (JsonProcessingException jpe) {
			logger.error("JsonProcessingException: {}", jpe.getMessage(), jpe);
			resultObj = new ApiError("USER_PREFERENCES_FAILED", "Failed to format user preferences records.");
		} catch (FeatureManagementException uare) {
			logger.error("FeatureManagementException: {}", uare.getMessage(), uare);
			resultObj = new ApiError("USER_PREFERENCES_FAILED", "Unable to locate user preferences records.");
		} finally {

			if (resultObj instanceof ApiError) {
				logger.error("FDAGndSvcLog> {}", ControllerUtils.sanitizeString(progressLog.toString()));
			}
		}

		return resultObj;
	}


	public byte[] getAllPreferencesFromSQL(String accessTokenInRequest, boolean includeUserPreferences) throws FeatureManagementException {

		StringBuilder preferencesBody = new StringBuilder();

		try {
			//Validate user
			final User user = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);
			List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
			if (airlineGroups.size() != 1) {
				throw new FeatureManagementException(new ApiError("FILE_DOWNLOAD_FAILURE", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED));
			}
			List<Group> roleGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_USER_ROLE_PREFIX)).collect(Collectors.toList());
			if (roleGroups.size() != 1) {
				throw new FeatureManagementException(new ApiError("FILE_DOWNLOAD_FAILURE", "Failed to associate user with a role", RequestFailureReason.UNAUTHORIZED));
			}

			String userRole = roleGroups.get(0).getDisplayName();
			String airlineGroup = airlineGroups.get(0).getDisplayName();

			// Validate the user role specified
			if (!Constants.ALLOWED_USER_ROLES.contains(userRole)) {
				logger.error("Missing or invalid user role specified in request: {}", userRole);
				throw  new FeatureManagementException(new ApiError("FILE_DOWNLOAD_FAILURE", "Missing or invalid user role", RequestFailureReason.BAD_REQUEST));
			}

			logger.debug("airlineGroup = {}", airlineGroup);
			logger.debug("userRole = {}", userRole);

			List<AirlinePreferences> airlinePreferences = featureManagementDao.getAirlinePreferences(airlineGroup, false);
			preferencesBody.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
			preferencesBody.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\r\n");
			preferencesBody.append("<plist version=\"1.0\">\r\n").append("<dict>\r\n");
			preferencesBody.append("<key>AIDProtocolKey</key>\r\n");
			preferencesBody.append("<string>https</string>\r\n");
			preferencesBody.append("<key>AIDHostKey</key>\r\n");
			preferencesBody.append("<string>172.31.1.1</string>\r\n");
			for (AirlinePreferences ap : airlinePreferences)
			{
				preferencesBody.append("<key>" + ap.getAirlineKey() + "</key>\r\n");
				if (userRole.equals("role-airlinefocal")) {
					if (ap.isChoiceFocal()) {
						preferencesBody.append("<true/>\r\n");
					} else {
						preferencesBody.append("<false/>\r\n");
					}
				}
				if (userRole.equals("role-airlinepilot")) {
					if (ap.isChoicePilot()) {
						preferencesBody.append("<true/>\r\n");
					} else {
						preferencesBody.append("<false/>\r\n");
					}
				}
				if (userRole.equals("role-airlinecheckairman")) {
					if (ap.isChoiceCheckAirman()) {
						preferencesBody.append("<true/>\r\n");
					} else {
						preferencesBody.append("<false/>\r\n");
					}
				}
				if (userRole.equals("role-airlinemaintenance")) {
					if (ap.isChoiceMaintenance()) {
						preferencesBody.append("<true/>\r\n");
					} else {
						preferencesBody.append("<false/>\r\n");
					}
				}
				if (userRole.equals("role-airlineefbadmin")) {
					if (ap.isChoiceEfbAdmin()) {
						preferencesBody.append("<true/>\r\n");
					} else {
						preferencesBody.append("<false/>\r\n");
					}
				}

			}
			if (includeUserPreferences) {
				logger.debug("include user preferences.");
				List<UserPreferences> userPreferences = featureManagementDao.getUserPreferences(airlineGroup);
				for (UserPreferences up : userPreferences) {
					preferencesBody.append("<key>" + up.getUserKey() + "</key>\r\n");
					if (!up.isToggle()) {
						preferencesBody.append("<string>" + up.getValue() + "</string>\r\n");
					} else {
						preferencesBody.append("<"+ up.getValue() +"/>\r\n");
					}
				}
			}
			preferencesBody.append("</dict>\r\n").append("</plist>\r\n");

			logger.info("returned [{}] airline preferences records of Airline Group [{}]", airlinePreferences.size(), airlineGroup);

		} catch (FeatureManagementException fme) {
			logger.error("FeatureManagementException: {}", fme.getMessage(), fme);
			throw new FeatureManagementException(new ApiError("FILE_DOWNLOAD_FAILURE", String.format("Error retrieving preferences SQL: %s",  fme.getMessage()), RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
		return preferencesBody.toString().getBytes();
	}


	public Object getAirlinePreferences(String accessTokenInRequest) {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Get Airline Preferences -");

		try {

			// Get group membership of user issuing request. Ensure that user belongs to role-airlinefocal group
			// and one and only one airline group.
			User airlineFocalCurrentUser = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);

			String airline = "airline-unknown";

			// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
			List<Group> airlineGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());
			if (airlineGroups.size() != 1 ) {
				return new ApiError("AIRLINE_PREFERENCES_FAILED", "User membership is ambiguous, airlines[" + airlineGroups.size() + "]", RequestFailureReason.UNAUTHORIZED);
			} else {
				airline = airlineGroups.get(0).getDisplayName();
			}
			progressLog.append("\nRequesting user belongs to single airline group and airline focal role group");

			List<AirlinePreferences> airlinePreferences = featureManagementDao.getAirlinePreferences(airline, true);

			ObjectMapper mapper = new ObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.setSerializationInclusion(JsonInclude.Include.NON_NULL);

			logger.info("returned [{}] airline preferences records of Airline Group [{}]", airlinePreferences.size(), airline);

			resultObj = mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL).setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
					.writeValueAsString(airlinePreferences);
			progressLog.append("\nObtained members of group in SQL DB");

		} catch (JsonProcessingException jpe) {
			logger.error("JsonProcessingException: {}", jpe.getMessage(), jpe);
			resultObj = new ApiError("AIRLINE_PREFERENCES_FAILED", "Failed to format Airline Preference record.");
		} catch (FeatureManagementException uare) {
			logger.error("FeatureManagementException: {}", uare.getMessage(), uare);
			resultObj = new ApiError("AIRLINE_PREFERENCES_FAILED", "Unable to locate records.");
		} finally {

			if (resultObj instanceof ApiError) {
				logger.error("FDAGndSvcLog> {}", ControllerUtils.sanitizeString(progressLog.toString()));
			}
		}

		return resultObj;
	}


	public Object getUserPreferences(String accessTokenInRequest) {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Get User Preferences -");

		try {

			// Get group membership of user issuing request. Ensure that user belongs to role-airlinefocal group
			// and one and only one airline group.
			User airlineFocalCurrentUser = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);

			String airline = "airline-unknown";

			// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
			List<Group> airlineGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());
			if (airlineGroups.size() != 1 ) {
				return new ApiError("AIRLINE_PREFERENCES_FAILED", "User membership is ambiguous, airlines[" + airlineGroups.size() + "]", RequestFailureReason.UNAUTHORIZED);
			} else {
				airline = airlineGroups.get(0).getDisplayName();
			}
			progressLog.append("\nRequesting user belongs to single airline group and airline focal role group");

			List<UserPreferences> userPreferences = featureManagementDao.getUserPreferences(airline);

			ObjectMapper mapper = new ObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.setSerializationInclusion(JsonInclude.Include.NON_NULL);

			logger.info("returned [{}] user preferences records of Airline Group [{}]", userPreferences.size(), airline);

			resultObj = mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL).setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
					.writeValueAsString(userPreferences);
			progressLog.append("\nObtained members of group in SQL DB");

		} catch (JsonProcessingException jpe) {
			logger.error("JsonProcessingException: {}", jpe.getMessage(), jpe);
			resultObj = new ApiError("USER_PREFERENCES_FAILED", "Failed to format User Preference record.");
		} catch (FeatureManagementException uare) {
			logger.error("FeatureManagementException: {}", uare.getMessage(), uare);
			resultObj = new ApiError("USER_PREFERENCES_FAILED", "Unable to locate records.");
		} finally {

			if (resultObj instanceof ApiError) {
				logger.error("FDAGndSvcLog> {}", ControllerUtils.sanitizeString(progressLog.toString()));
			}
		}

		return resultObj;
	}



}
