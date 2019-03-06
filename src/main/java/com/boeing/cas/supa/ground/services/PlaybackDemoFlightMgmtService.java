package com.boeing.cas.supa.ground.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boeing.cas.supa.ground.dao.PlaybackDemoFlightMgmtDao;
import com.boeing.cas.supa.ground.exceptions.PlaybackDemoFlightException;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.Group;
import com.boeing.cas.supa.ground.pojos.PlaybackDemoFlight;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.boeing.cas.supa.ground.utils.ControllerUtils;

@Service
public class PlaybackDemoFlightMgmtService {

	private final Logger logger = LoggerFactory.getLogger(PlaybackDemoFlightMgmtService.class);

	private static String DEMO_FLIGHTS_CONTAINER = "demo-flight-streams";

	@Autowired
	private Map<String, String> appProps;

	@Autowired
	private AzureADClientService aadClient;

	@Autowired
	private PlaybackDemoFlightMgmtDao playbackDemoFlightMgmtDao;

	public List<PlaybackDemoFlight> listDemoFlightStreams(String authToken) throws PlaybackDemoFlightException {

		List<PlaybackDemoFlight> playbackDemoFlights = new ArrayList<>();

		// Determine the airline from the user's membership.
		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			throw new PlaybackDemoFlightException(new ApiError("DEMO_FLIGHT_MGMT_LIST", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED));
		}
		//String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);
		//logger.debug("User airline for retrieving SUPA releases is [{}]", airlineGroup);
		// This is presently accessible only to users in the FDA airline group
		//if (!"FDA".equalsIgnoreCase(airlineGroup)) {
		//	logger.error("Non-FDA users [{}] are not allowed to playback demo flights", airlineGroup);
		//	throw new PlaybackDemoFlightException(new ApiError("DEMO_FLIGHT_MGMT_LIST", "Only FDA users are allowed to playback demo flights", RequestFailureReason.UNAUTHORIZED));
		//}

		playbackDemoFlights = playbackDemoFlightMgmtDao.listDemoFlightStreams();
		
		return CollectionUtils.isEmpty(playbackDemoFlights) ? new ArrayList<PlaybackDemoFlight>() : playbackDemoFlights;
	}

	public PlaybackDemoFlight getDemoFlightStream(String authToken, String flightStreamName) throws PlaybackDemoFlightException {

		// Determine the airline from the user's membership.
		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			throw new PlaybackDemoFlightException(new ApiError("DEMO_FLIGHT_MGMT_DOWNLOAD", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED));
		}
		String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);
		logger.debug("User airline for retrieving SUPA releases is [{}]", airlineGroup);
		// This is presently accessible only to users in the FDA airline group
		if (!"FDA".equalsIgnoreCase(airlineGroup)) {
			logger.error("Non-FDA users [{}] are not allowed to playback demo flights", airlineGroup);
			throw new PlaybackDemoFlightException(new ApiError("DEMO_FLIGHT_MGMT_RETRIEVE", "Only FDA users are allowed to access demo flights", RequestFailureReason.UNAUTHORIZED));
		}

		try {

			AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"), this.appProps.get("StorageKey"));
			if (!StringUtils.isBlank(flightStreamName)) {

				PlaybackDemoFlight playbackDemoFlight = playbackDemoFlightMgmtDao.getDemoFlightStream(flightStreamName);
				if (playbackDemoFlight != null) {

					try (ByteArrayOutputStream outputStream = asu.downloadFile(DEMO_FLIGHTS_CONTAINER, playbackDemoFlight.getPath())) {

						if (outputStream != null) {
							outputStream.flush();
							playbackDemoFlight.setFile(outputStream.toByteArray());
							return playbackDemoFlight;
						}
					}
				} else {
					logger.error("Failed to retrieve specified demo flight stream: {}", ControllerUtils.sanitizeString(flightStreamName));
					throw new PlaybackDemoFlightException(new ApiError("DEMO_FLIGHT_MGMT_DOWNLOAD", String.format("Failed to retrieve specified demo flight stream: %s", ControllerUtils.sanitizeString(flightStreamName)), RequestFailureReason.INTERNAL_SERVER_ERROR));
				}
			}
		} catch (IOException e) {
			logger.error("Error retrieving demo flight stream [{}]: {}", ControllerUtils.sanitizeString(flightStreamName),
					e.getMessage(), e);
			throw new PlaybackDemoFlightException(new ApiError("DEMO_FLIGHT_MGMT_DOWNLOAD", String.format("Failed to retrieve specified demo flight stream: %s", ControllerUtils.sanitizeString(flightStreamName)), RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

		return null;
	}
}
