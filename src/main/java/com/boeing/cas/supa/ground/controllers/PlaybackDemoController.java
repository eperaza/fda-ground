package com.boeing.cas.supa.ground.controllers;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.boeing.cas.supa.ground.exceptions.PlaybackDemoFlightException;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.PlaybackDemoFlight;
import com.boeing.cas.supa.ground.services.PlaybackDemoFlightMgmtService;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.boeing.cas.supa.ground.utils.ControllerUtils;

@RestController
@RequestMapping(path="/playback")
public class PlaybackDemoController {

	private final Logger logger = LoggerFactory.getLogger(PlaybackDemoController.class);

	@Autowired
	private PlaybackDemoFlightMgmtService playbackDemoService;

	@RequestMapping(path="/list", method = { RequestMethod.GET }, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> listSupaReleases(@RequestHeader("Authorization") String authToken) {

		CacheControl cacheControl = CacheControl.maxAge(0, TimeUnit.SECONDS);

		try {
			List<PlaybackDemoFlight> demoFlightStreams = this.playbackDemoService.listDemoFlightStreams(authToken);
			logger.debug("{} demo flight(s)", CollectionUtils.isEmpty(demoFlightStreams) ? 0 : demoFlightStreams.size());
			return ResponseEntity.ok()
					.cacheControl(cacheControl)
					.contentType(MediaType.APPLICATION_JSON_UTF8)
					.body(demoFlightStreams);
		} catch (PlaybackDemoFlightException pdfe) {
			logger.error("Failed to retrieve list of demo flights: {}", pdfe.getMessage(), pdfe);
			return new ResponseEntity<>(new ApiError("DEMO_FLIGHTS_MGMT_LIST", pdfe.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping(value = "/getDemoFlight/{flightStreamName}")
	public ResponseEntity<Object> getRelease(@RequestHeader("Authorization") String authToken,
			@PathVariable String flightStreamName,
			HttpServletResponse response) {

		CacheControl cacheControl = CacheControl.maxAge(0, TimeUnit.SECONDS);
		
		PlaybackDemoFlight demoFlightStream = null;
		try {
			demoFlightStream = this.playbackDemoService.getDemoFlightStream(authToken, flightStreamName);
			if (demoFlightStream != null && demoFlightStream.getFile() != null && demoFlightStream.getFile().length > 0) {
				return ResponseEntity.ok()
						.cacheControl(cacheControl)
		                .contentType(MediaType.APPLICATION_OCTET_STREAM)
		                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + demoFlightStream.getPath() + "\"")
		                .body(demoFlightStream.getFile());
			}

			// Throw exception if this point is reached
			throw new PlaybackDemoFlightException(new ApiError("PLAYBACK_DEMO_FLIGHT_DOWNLOAD", "Missing or invalid Demo Flight Stream name", RequestFailureReason.BAD_REQUEST));
		} catch (PlaybackDemoFlightException pdfe) {

			logger.error("Failed to retrieve specified demo flight stream [{}]: {}", ControllerUtils.sanitizeString(flightStreamName), pdfe.getMessage(), pdfe);
			//return new ResponseEntity<>(pdfe.getError(), ControllerUtils.translateRequestFailureReasonToHttpErrorCode(pdfe.getError().getFailureReason()));
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pdfe.getError().getErrorDescription());
		}
	}
}
