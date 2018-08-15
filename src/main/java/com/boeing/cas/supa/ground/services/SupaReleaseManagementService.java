package com.boeing.cas.supa.ground.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boeing.cas.supa.ground.exceptions.SupaReleaseException;
import com.boeing.cas.supa.ground.pojos.Group;
import com.boeing.cas.supa.ground.pojos.SupaRelease;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.ControllerUtils;

@Service
public class SupaReleaseManagementService {

	private final Logger logger = LoggerFactory.getLogger(SupaReleaseManagementService.class);

	private static String SUPA_RELEASE_CONTAINER = "supa-releases";
	private static List<SupaRelease> SUPA_RELEASES;
	static {

		SUPA_RELEASES = new ArrayList<>();

		SUPA_RELEASES.add(new SupaRelease("5.4.1", "BFB56-ASUP-0016", "supa-5.4.1-1.noarch.rpm", "AMX"));
		SUPA_RELEASES.add(new SupaRelease("5.4.1", "BFB56-ASUP-0016", "supa-5.4.1-1.noarch.rpm", "EFO"));
		SUPA_RELEASES.add(new SupaRelease("5.4.1", "BFB56-ASUP-0016", "supa-5.4.1-1.noarch.rpm", "FDA"));
		SUPA_RELEASES.add(new SupaRelease("5.4.1", "BFB56-ASUP-0016", "supa-5.4.1-1.noarch.rpm", "SLK"));
		SUPA_RELEASES.add(new SupaRelease("5.4.1", "BFB56-ASUP-0016", "supa-5.4.1-1.noarch.rpm", "UAL"));

		SUPA_RELEASES.add(new SupaRelease("5.4.2", "BFB57-ASUP-0017", "supa-5.4-2.noarch.rpm", "AMX"));
		SUPA_RELEASES.add(new SupaRelease("5.4.2", "BFB57-ASUP-0017", "supa-5.4-2.noarch.rpm", "EFO"));
		SUPA_RELEASES.add(new SupaRelease("5.4.2", "BFB57-ASUP-0017", "supa-5.4-2.noarch.rpm", "FDA"));
		SUPA_RELEASES.add(new SupaRelease("5.4.2", "BFB57-ASUP-0017", "supa-5.4-2.noarch.rpm", "SLK"));
		SUPA_RELEASES.add(new SupaRelease("5.4.2", "BFB57-ASUP-0017", "supa-5.4-2.noarch.rpm", "UAL"));
	}

	@Autowired
	private Map<String, String> appProps;

	@Autowired
	private AzureADClientService aadClient;

	public List<SupaRelease> listSupaReleases(String authToken) throws SupaReleaseException {

		// Determine the airline from the user's membership.
		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			throw new SupaReleaseException("Failed to associate user with an airline");
		}
		String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);
		logger.debug("User airline for retrieving SUPA releases is [{}]", airlineGroup);
		List<SupaRelease> listOfSupaReleases = SUPA_RELEASES
					.parallelStream()
					.filter(sr -> !StringUtils.isBlank(sr.getAirline()) && sr.getAirline().toLowerCase().equals(airlineGroup))
					.map(sr -> { sr.setFile(null); return sr; })
					.collect(Collectors.toList());

		return listOfSupaReleases;
	}

	public SupaRelease getSupaRelease(String authToken, String releaseVersion) throws SupaReleaseException {

		// Determine the airline from the user's membership.
		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			throw new SupaReleaseException("Failed to associate user with an airline");
		}
		String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);
		logger.debug("User airline for retrieving SUPA releases is [{}]", airlineGroup);

		try {

			AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"), this.appProps.get("StorageKey"));
			if (!StringUtils.isBlank(releaseVersion)) {

				Optional<SupaRelease> supaRelease = SUPA_RELEASES.parallelStream()
						.filter(sr -> sr.getRelease().equals(releaseVersion) && sr.getAirline().toLowerCase().equals(airlineGroup))
						.findFirst();
				if (supaRelease.isPresent()) {
					String supaReleasePath = supaRelease.get().getPath();
					try (ByteArrayOutputStream outputStream = asu.downloadFile(SUPA_RELEASE_CONTAINER, new StringBuilder(releaseVersion).append('/').append(supaReleasePath).toString())) {
						
						if (outputStream != null) {
							SupaRelease releaseFile = supaRelease.get();
							outputStream.flush();
							releaseFile.setFile(outputStream.toByteArray());
							return releaseFile;
						}
					}
				} else {
					logger.error("Failed to retrieve specified SUPA release: {}", ControllerUtils.sanitizeString(releaseVersion));
				}
			}
		} catch (IOException e) {
			logger.error("ApiError retrieving SUPA release version [{}]: {}", ControllerUtils.sanitizeString(releaseVersion),
					e.getMessage(), e);
		}

		return null;
	}

	public SupaRelease getSupaReleaseChecksum(String authToken, String releaseVersion) throws SupaReleaseException {

		// Determine the airline from the user's membership.
		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			throw new SupaReleaseException("Failed to associate user with an airline");
		}
		String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);
		logger.debug("User airline for retrieving SUPA releases is [{}]", airlineGroup);

		try {

			AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"), this.appProps.get("StorageKey"));
			if (!StringUtils.isBlank(releaseVersion)) {

				Optional<SupaRelease> supaRelease = SUPA_RELEASES.parallelStream()
						.filter(sr -> sr.getRelease().equals(releaseVersion) && sr.getAirline().toLowerCase().equals(airlineGroup))
						.findFirst();
				if (supaRelease.isPresent()) {
					String supaReleasePath = supaRelease.get().getPath();
					try (ByteArrayOutputStream outputStream = asu.downloadFile(SUPA_RELEASE_CONTAINER, new StringBuilder(releaseVersion).append('/').append(supaReleasePath).append(Constants.CHECKSUM_PREFIX_SHA1).toString())) {
						
						if (outputStream != null) {
							SupaRelease releaseFile = supaRelease.get();
							outputStream.flush();
							releaseFile.setFile(outputStream.toByteArray());
							return releaseFile;
						}
					}
				} else {
					logger.error("Failed to retrieve specified SUPA release checksum: {}", ControllerUtils.sanitizeString(releaseVersion));
				}
			}
		} catch (IOException e) {
			logger.error("ApiError retrieving SUPA release version checksum[{}]: {}", ControllerUtils.sanitizeString(releaseVersion),
					e.getMessage(), e);
		}

		return null;
	}
}
