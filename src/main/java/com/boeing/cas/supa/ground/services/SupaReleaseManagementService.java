package com.boeing.cas.supa.ground.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.boeing.cas.supa.ground.pojos.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boeing.cas.supa.ground.dao.SupaReleaseManagementDao;
import com.boeing.cas.supa.ground.exceptions.SupaReleaseException;
import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.boeing.cas.supa.ground.utils.ControllerUtils;

@Service
public class SupaReleaseManagementService {

	private final Logger logger = LoggerFactory.getLogger(SupaReleaseManagementService.class);

	private static String SUPA_RELEASE_CONTAINER = "supa-releases";
	private static String WAR_RELEASE_CONTAINER = "war-releases";

	@Autowired
	private Map<String, String> appProps;

	@Autowired
	private AzureADClientService aadClient;

	@Autowired
	private SupaReleaseManagementDao supaReleaseManagementDao;

	public List<SupaRelease> listSupaReleases(String authToken, short versions) throws SupaReleaseException {

		List<SupaRelease> listOfSupaReleases = new ArrayList<>();

		try {

			// Determine the airline from the user's membership.
			final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
			List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
			if (airlineGroups.size() != 1) {
				throw new SupaReleaseException(new ApiError("SUPA_RELEASE_MGMT", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED));
			}
			String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);
			logger.debug("User airline for retrieving SUPA releases is [{}]", airlineGroup);
			listOfSupaReleases = supaReleaseManagementDao.getSupaReleases(airlineGroup.toLowerCase());

			return listOfSupaReleases.subList(0, ((versions < listOfSupaReleases.size()) ? versions : listOfSupaReleases.size()));

		} catch (IllegalArgumentException iae) {
			logger.error("Probable missing or invalid version count [{}]: {}", versions, iae.getMessage());
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_MGMT", "Missing or invalid release count requested", RequestFailureReason.BAD_REQUEST));
		}
	}

	public List<SupaRelease> listWarReleases(String authToken, short versions) throws SupaReleaseException {

		List<SupaRelease> listOfWarReleases = new ArrayList<>();

		try {

			// Determine the airline from the user's membership.
			final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
			List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
			if (airlineGroups.size() != 1) {
				throw new SupaReleaseException(new ApiError("SUPA_RELEASE_MGMT", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED));
			}
			String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);
			logger.debug("User airline for retrieving WAR releases is [{}]", airlineGroup);
			listOfWarReleases = supaReleaseManagementDao.getWarReleases(airlineGroup.toLowerCase());

			return listOfWarReleases.subList(0, ((versions < listOfWarReleases.size()) ? versions : listOfWarReleases.size()));

		} catch (IllegalArgumentException iae) {
			logger.error("Probable missing or invalid version count [{}]: {}", versions, iae.getMessage());
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_MGMT", "Missing or invalid release count requested", RequestFailureReason.BAD_REQUEST));
		}
	}


	public SupaRelease getSupaRelease(String authToken, String releaseVersion) throws SupaReleaseException {

		// Determine the airline from the user's membership.
		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_DOWNLOAD", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED));
		}
		String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);
		logger.debug("User airline for retrieving SUPA releases is [{}]", airlineGroup);

		try {

			AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"), this.appProps.get("StorageKey"));
			if (!StringUtils.isBlank(releaseVersion)) {

				SupaRelease supaRelease = supaReleaseManagementDao.getSupaReleaseByRelease(releaseVersion, airlineGroup.toLowerCase());
				if (supaRelease != null) {

					try (ByteArrayOutputStream outputStream = asu.downloadFile(SUPA_RELEASE_CONTAINER, new StringBuilder(releaseVersion).append('/').append(supaRelease.getPath()).toString())) {

						if (outputStream != null) {
							outputStream.flush();
							supaRelease.setFile(outputStream.toByteArray());
							return supaRelease;
						}
					}
				} else {
					logger.error("Failed to retrieve specified SUPA release: {}", ControllerUtils.sanitizeString(releaseVersion));
					throw new SupaReleaseException(new ApiError("SUPA_RELEASE_DOWNLOAD", String.format("Failed to retrieve specified SUPA release: %s", ControllerUtils.sanitizeString(releaseVersion)), RequestFailureReason.INTERNAL_SERVER_ERROR));
				}
			}
		} catch (IOException e) {
			logger.error("ApiError retrieving SUPA release version [{}]: {}", ControllerUtils.sanitizeString(releaseVersion),
					e.getMessage(), e);
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_DOWNLOAD", String.format("Failed to retrieve specified SUPA release: %s", ControllerUtils.sanitizeString(releaseVersion)), RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

		return null;
	}

	public Object getCurrentSupaRelease(String accessTokenInRequest) {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Get Current Supa Release - ");
		try {
			User currentUser = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);
			String airline = "unknown";

			// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
			List<Group> airlineGroups = currentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
			if (airlineGroups.size() != 1) {
				return new ApiError("SUPA_RELEASE_MGMT", "User membership is ambiguous, airlines[" + airlineGroups.size() + "]", RequestFailureReason.UNAUTHORIZED);
			}
			airline = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);

			// Obtain Current Supa version for airline
			logger.info("Obtain Current Supa Version for airline: " + airline);
			List<CurrentSupaRelease> currentSupaReleaseList = supaReleaseManagementDao.getCurrentSupaRelease(airline);

			logger.info("Current Supa Version is [{}]" , currentSupaReleaseList.size());

			ObjectMapper mapper = new ObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.setSerializationInclusion(JsonInclude.Include.NON_NULL);

			logger.info("returned Current Supa Release[{}] for Airline[{}]", currentSupaReleaseList.size(), airline);

			resultObj = mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL).setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
					.writeValueAsString(currentSupaReleaseList);
			progressLog.append("\nObtained members of group in SQL DB");

		} catch (JsonProcessingException jpe) {
			logger.error("JsonProcessingException: {}", jpe.getMessage(), jpe);
			resultObj = new ApiError("SUPA_RELEASE_MGMT", "Failed to format Current Supa Release record.");

		} catch (SupaReleaseException jpe) {
			logger.error("SupaReleaseException: {}", jpe.getMessage(), jpe);
			resultObj = new ApiError("SUPA_RELEASE_MGMT", jpe.getMessage());
		} finally {

			if (resultObj instanceof ApiError) {
				logger.error("FDAGndSvcLog> {}", ControllerUtils.sanitizeString(progressLog.toString()));
			}
		}
		return resultObj;
	}

	public Object setCurrentSupaRelease(String accessTokenInRequest, String version) {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Set Current Supa Release - " + version);

		try {

			// Get group membership of user issuing request. Ensure that user belongs to role-airlinefocal group
			// and one and only one airline group.
			User currentUser = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);

			String airline = "unknown";

			// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
			List<Group> airlineGroups = currentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
			if (airlineGroups.size() != 1) {
				return new ApiError("SUPA_RELEASE_MGMT", "User membership is ambiguous, airlines[" + airlineGroups.size() + "]", RequestFailureReason.UNAUTHORIZED);
			}
			airline = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);

			// Check to see if release exists in war release table for airline
			logger.info("Check to see if supa.war version [{}] exists for airline: {}", version, airline);
			// note: use NA to remove all version
			if (!version.equalsIgnoreCase("NA")) {
				SupaRelease warRelease = supaReleaseManagementDao.getWarReleaseByRelease(version, airline);
				if (warRelease == null) {
					return new ApiError("SUPA_RELEASE_MGMT", "War release [" + version + "] does not exist for airline[" + airline + "]", RequestFailureReason.UNAUTHORIZED);
				}
			}

			progressLog.append("\nRequesting user belongs to single airline group and airline focal role group");
			// Remove current version (if there)
			logger.info("Remove Current Supa Version for airline [{}]", version, airline);
			supaReleaseManagementDao.removeCurrentSupaRelease(airline);

			// Insert current version if not equal to NA
			if (!version.equalsIgnoreCase("NA")) {
				CurrentSupaRelease currentSupaRelease = new CurrentSupaRelease(version, "current release: " + version + " for " + airline,
						currentUser.getUserPrincipalName(), airline);
				supaReleaseManagementDao.insertCurrentSupaRelease(currentSupaRelease);
			}

			// Obtained inserted version
			List<CurrentSupaRelease> currentSupaReleaseList = supaReleaseManagementDao.getCurrentSupaRelease(airline);

			ObjectMapper mapper = new ObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.setSerializationInclusion(JsonInclude.Include.NON_NULL);

			logger.info("returned Current Supa Release[{}] for Airline[{}]", currentSupaReleaseList.size(), airline);

			resultObj = mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL).setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
					.writeValueAsString(currentSupaReleaseList);
			progressLog.append("\nObtained members of group in SQL DB");

		} catch (JsonProcessingException jpe) {
			logger.error("JsonProcessingException: {}", jpe.getMessage(), jpe);
			resultObj = new ApiError("SUPA_RELEASE_MGMT", "Failed to format Current Supa Release record.");

		} catch (SupaReleaseException jpe) {
			logger.error("SupaReleaseException: {}", jpe.getMessage(), jpe);
			resultObj = new ApiError("SUPA_RELEASE_MGMT", jpe.getMessage());
		} finally {

			if (resultObj instanceof ApiError) {
				logger.error("FDAGndSvcLog> {}", ControllerUtils.sanitizeString(progressLog.toString()));
			}
		}
		return resultObj;
	}



	public SupaRelease getWarRelease(String authToken, String releaseVersion) throws SupaReleaseException {

		// Determine the airline from the user's membership.
		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_DOWNLOAD", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED));
		}
		String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);
		logger.debug("User airline for retrieving WAR releases is [{}]", airlineGroup);

		try {

			AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"), this.appProps.get("StorageKey"));
			if (!StringUtils.isBlank(releaseVersion)) {

				SupaRelease supaRelease = supaReleaseManagementDao.getWarReleaseByRelease(releaseVersion, airlineGroup.toLowerCase());
				if (supaRelease != null) {

					try (ByteArrayOutputStream outputStream = asu.downloadFile(WAR_RELEASE_CONTAINER,
						new StringBuilder(releaseVersion).append('/').append(supaRelease.getPath()).toString())) {

						if (outputStream != null) {
							outputStream.flush();
							supaRelease.setFile(outputStream.toByteArray());
							return supaRelease;
						}
					}
				} else {
					logger.error("Failed to retrieve specified WAR release: {}", ControllerUtils.sanitizeString(releaseVersion));
					throw new SupaReleaseException(new ApiError("SUPA_RELEASE_DOWNLOAD", String.format("Failed to retrieve specified WAR release: %s",
						ControllerUtils.sanitizeString(releaseVersion)), RequestFailureReason.INTERNAL_SERVER_ERROR));
				}
			}
		} catch (IOException e) {
			logger.error("ApiError retrieving WAR release version [{}]: {}", ControllerUtils.sanitizeString(releaseVersion),
					e.getMessage(), e);
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_DOWNLOAD", String.format("Failed to retrieve specified WAR release: %s", ControllerUtils.sanitizeString(releaseVersion)), RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

		return null;
	}

}
