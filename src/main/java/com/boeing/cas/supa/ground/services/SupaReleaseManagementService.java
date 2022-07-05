package com.boeing.cas.supa.ground.services;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.boeing.cas.supa.ground.pojos.*;
import com.boeing.cas.supa.util.SupaSecretUtils;
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
import com.boeing.cas.supa.ground.utils.IOUtils;
import com.boeing.cas.supa.ground.utils.KeyVaultRetriever;


@Service
public class SupaReleaseManagementService {

	private final Logger logger = LoggerFactory.getLogger(SupaReleaseManagementService.class);

	private static String SUPA_RELEASE_CONTAINER = "supa-releases";
	private static String WAR_RELEASE_CONTAINER = "war-releases";
	private static final String SUPA_WAR = "supa.war";
	private static final String SUPA_WAR_CHECKSUM = "supa.war.sha256";
	private static final String START_ENCRYPT_SUPA_VERSION = "6.0";
	private static final String VERSION_SEPERATOR = "\\.";

	@Autowired
	private Map<String, String> appProps;

	@Autowired
	private AzureADClientService aadClient;

	@Autowired
	private SupaReleaseManagementDao supaReleaseManagementDao;
	
	@Autowired
	private KeyVaultRetriever keyVaultRetriever;
	
	@Autowired
	private ZipService zipService;

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

	public Object getZuppa(String accessTokenInRequest) {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Get zuppa");
		try {
			User currentUser = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);
			String airline = "unknown";

			// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
			List<Group> airlineGroups = currentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
			if (airlineGroups.size() != 1) {
				return new ApiError("SUPA_RELEASE_MGMT", "User membership is ambiguous, airlines[" + airlineGroups.size() + "]", RequestFailureReason.UNAUTHORIZED);
			}

			airline = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);

			logger.info("Getting zupa for zuppa-" + airline);
			StringBuilder zuppa = new StringBuilder(appProps.get("zuppa-" + airline));

			char[] zuppaChar = zuppa.toString().toCharArray();
			resultObj = SupaSecretUtils.generatePassword(zuppa.toString().toCharArray(), (short) 1004);

			ObjectMapper mapper = new ObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		} catch (Exception exception) {
			logger.error("Get Zuppa exception: {}", exception.getMessage(), exception);
			resultObj = new ApiError("SUPA_RELEASE_MGMT", exception.getMessage());
		} finally {

			if (resultObj instanceof ApiError) {
				logger.error("FDAGndSvcLog> {}", ControllerUtils.sanitizeString(progressLog.toString()));
			}
		}
		return resultObj;
	}

	public Object getZuppa2(String accessTokenInRequest) {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Get zuppa2");
		try {
			User currentUser = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);
			String airline = "unknown";

			// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
			List<Group> airlineGroups = currentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
			if (airlineGroups.size() != 1) {
				return new ApiError("SUPA_RELEASE_MGMT", "User membership is ambiguous, airlines[" + airlineGroups.size() + "]", RequestFailureReason.UNAUTHORIZED);
			}

			airline = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);

			logger.info("Getting raw zupa for zuppa-" + airline);
			StringBuilder zuppa = new StringBuilder(appProps.get("zuppa-" + airline));

			resultObj = zuppa.toString();

			ObjectMapper mapper = new ObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		} catch (Exception exception) {
			logger.error("Get Zuppa2 exception: {}", exception.getMessage(), exception);
			resultObj = new ApiError("SUPA_RELEASE_MGMT", exception.getMessage());
		} finally {

			if (resultObj instanceof ApiError) {
				logger.error("FDAGndSvcLog> {}", ControllerUtils.sanitizeString(progressLog.toString()));
			}
		}
		return resultObj;
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
			List<CurrentSupaRelease> currentSupaReleaseList = supaReleaseManagementDao.getCurrentSupaRelease(airline);

			logger.info("Current Supa Release for airline [{}] is [{}]",
				airline, currentSupaReleaseList.size()>0 ? currentSupaReleaseList.get(0).getRelease() : "empty");

			ObjectMapper mapper = new ObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.setSerializationInclusion(JsonInclude.Include.NON_NULL);

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
			// note: use NA to remove all version
			if (!version.equalsIgnoreCase("NA")) {
				SupaRelease warRelease = supaReleaseManagementDao.getWarReleaseByRelease(version, airline);
				if (warRelease == null) {
					return new ApiError("SUPA_RELEASE_MGMT", "War release [" + version + "] does not exist for airline[" + airline + "]", RequestFailureReason.UNAUTHORIZED);
				}
			}

			progressLog.append("\nRequesting user belongs to single airline group and airline focal role group");
			// Remove current version (if there)
			logger.info("Remove Current Supa Version for airline: [{}]", airline);
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

			logger.info("Set Current Supa Release for airline [{}] is [{}]",
					airline, currentSupaReleaseList.size()>0 ? currentSupaReleaseList.get(0).getRelease() : "empty");

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
		return getWarRelease(authToken, releaseVersion, false);
	}

	public SupaRelease getWarRelease(String authToken, String releaseVersion, Boolean forceUnEncrypted) throws SupaReleaseException {

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
							if (forceUnEncrypted) {
								getWarReleaseContent(airlineGroup, forceUnEncrypted, supaRelease, outputStream);
							} else {
								getWarReleaseContent(airlineGroup, supaRelease, outputStream);
							}
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
			throw new SupaReleaseException(new ApiError("WAR_RELEASE_DOWNLOAD", String.format("Failed to retrieve specified WAR release: %s", ControllerUtils.sanitizeString(releaseVersion)), RequestFailureReason.INTERNAL_SERVER_ERROR));
		} catch (SupaReleaseException se) {
			logger.error("SupaRelease Exception: {}", se.getMessage(), se);
			throw new SupaReleaseException(new ApiError("WAR_RELEASE_DOWNLOAD", String.format("Failed to retrieve specified WAR release: %s", ControllerUtils.sanitizeString(releaseVersion)), RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
		return null;
	}
	
	private void getWarReleaseContent(String airlineName, SupaRelease supaRelease, ByteArrayOutputStream warFileContent) throws IOException {
		boolean needUnEncryptedContent = compareSupaVersions(supaRelease.getRelease(), START_ENCRYPT_SUPA_VERSION) < 0;
		logger.info("Comparing supa versions {} {}", supaRelease.getRelease(), START_ENCRYPT_SUPA_VERSION);
		logger.info("Determined we need unencrypted? {}", needUnEncryptedContent);
		getWarReleaseContent(airlineName, needUnEncryptedContent, supaRelease, warFileContent);
	}

	private void getWarReleaseContent(String airlineName, Boolean forceUnencrypted, SupaRelease supaRelease, ByteArrayOutputStream warFileContent) throws IOException {

		if (forceUnencrypted) {
			logger.info("Sending unencrypted file");
			supaRelease.setFile(warFileContent.toByteArray());
			return;
		} else {
			logger.info("Encrypting supa and sending");
			// do checksum, encrypted zip up content
			File downloadedFile = IOUtils.writeToTempFile(warFileContent, SUPA_WAR);
			File tempDir = downloadedFile.getParentFile();
			File checksumFile = new File(tempDir, SUPA_WAR_CHECKSUM);
			IOUtils.writeToFile(IOUtils.getChecksumSHA256InByteArray(downloadedFile), checksumFile, false);

			File encryptedFile = new File(tempDir, "supa.zip");
			String password = keyVaultRetriever.getSecretByKey(String.format("%s%s", Constants.ZUPPA_SECRET_PREFIX, airlineName));
			encryptedFile = zipService.zipFilesEncrypted(password, encryptedFile.toString(), null, downloadedFile, checksumFile);

			supaRelease.setFile(Files.readAllBytes(encryptedFile.toPath()));

			// delete temp folder
			IOUtils.deleteDirQuietly(tempDir);
		}
	}
	
	private int compareSupaVersions(String version1, String version2) {
		String[] versionParts1 = version1.split(VERSION_SEPERATOR);
		String[] versionParts2 = version2.split(VERSION_SEPERATOR);
		return compareSupaVersions(versionParts1, versionParts2, 0);
	}
	
	private int compareSupaVersions(String[] versionParts1, String[] versionParts2, int index) {
		if (versionParts1.length <= index) {
			return versionParts2.length >= index ? -1 : 0;
		}
		
		if (versionParts2.length <= index) {
			return versionParts1.length >= index ? 1 : 0;
		}
		
		int versionNumber1 = Integer.valueOf(versionParts1[index]);
		int versionNumber2 = Integer.valueOf(versionParts2[index]);
		if (versionNumber1 != versionNumber2) {
			return Integer.compare(versionNumber1, versionNumber2);
		}
		
		return compareSupaVersions(versionParts1, versionParts2, index + 1);
	}
}
