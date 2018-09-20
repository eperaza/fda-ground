package com.boeing.cas.supa.ground.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boeing.cas.supa.ground.dao.SupaReleaseManagementDao;
import com.boeing.cas.supa.ground.exceptions.SupaReleaseException;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.Group;
import com.boeing.cas.supa.ground.pojos.SupaRelease;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.boeing.cas.supa.ground.utils.ControllerUtils;

@Service
public class SupaReleaseManagementService {

	private final Logger logger = LoggerFactory.getLogger(SupaReleaseManagementService.class);

	private static String SUPA_RELEASE_CONTAINER = "supa-releases";

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
}
