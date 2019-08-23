package com.boeing.cas.supa.ground.controllers;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import com.boeing.cas.supa.ground.pojos.CurrentSupaRelease;
import com.boeing.cas.supa.ground.utils.Constants;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boeing.cas.supa.ground.exceptions.SupaReleaseException;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.SupaRelease;
import com.boeing.cas.supa.ground.services.SupaReleaseManagementService;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.boeing.cas.supa.ground.utils.ControllerUtils;

@RestController
@RequestMapping(path="/supa-release-mgmt")
public class SupaMgmtController {

	private final Logger logger = LoggerFactory.getLogger(SupaMgmtController.class);

	@Autowired
	private SupaReleaseManagementService supaReleaseMgmtService;

	@RequestMapping(path="/list", method = { RequestMethod.GET }, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> listSupaReleases(@RequestHeader("Authorization") String authToken,
												   @RequestParam short versions,
												   @RequestParam Optional<String> fileType) {

		CacheControl cacheControl = CacheControl.maxAge(0, TimeUnit.SECONDS);
		logger.info("Number of versions to return [{}]", versions);
		logger.info("Filetype is present? [{}]", fileType.isPresent());
		try {
			List<SupaRelease> releases = null;
			if (fileType.isPresent()) {
				//if present, return war file
				releases = this.supaReleaseMgmtService.listWarReleases(authToken, versions);
			} else {
				releases = this.supaReleaseMgmtService.listSupaReleases(authToken, versions);
			}
			logger.debug("{} release(s)", CollectionUtils.isEmpty(releases) ? 0 : releases.size());
			return ResponseEntity.ok()
					.cacheControl(cacheControl)
					.contentType(MediaType.APPLICATION_JSON_UTF8)
					.body(releases);
		} catch (SupaReleaseException sre) {
			logger.error("Failed to retrieve list of SUPA releases: {}", sre.getMessage(), sre);
			return new ResponseEntity<>(new ApiError("SUPA_RELEASE_LIST", sre.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@RequestMapping(path="/getCurrentSupaRelease", method = { RequestMethod.GET })
	public ResponseEntity<Object> getCurrentSupaRelease(@RequestHeader("Authorization") String authToken) {

		// Extract the access token from the authorization request header
		String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);

		// Get Current Supa Release for fleet
		Object result = supaReleaseMgmtService.getCurrentSupaRelease(accessTokenInRequest);

		if (result instanceof ApiError) {
			return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
		}
		return new ResponseEntity<>(result, HttpStatus.OK);
	}


	@RequestMapping(path = "/setCurrentSupaRelease", method = { RequestMethod.POST })
	public ResponseEntity<Object> setCurrentSupaRelease(final @RequestParam("version") String releaseVersion,
													@RequestHeader("Authorization") String authToken) {

		logger.info("set Current Supa release to [{}]", releaseVersion);

		// Set current Supa Release for airline
		Object result = supaReleaseMgmtService.setCurrentSupaRelease(authToken, releaseVersion);

		if (result instanceof ApiError) {
			return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
		}

		return new ResponseEntity<>(result, HttpStatus.OK);
	}


	@GetMapping(value = "/getRelease/{version:.+}")
	public ResponseEntity<Object> getRelease(@RequestHeader("Authorization") String authToken,
			@PathVariable("version") String releaseVersion,
			HttpServletResponse response) {

		CacheControl cacheControl = CacheControl.maxAge(0, TimeUnit.SECONDS);
		
		SupaRelease supaRelease = null;
		try {
			supaRelease = supaReleaseMgmtService.getSupaRelease(authToken, releaseVersion);
			if (supaRelease != null && supaRelease.getFile() != null && supaRelease.getFile().length > 0) {
				return ResponseEntity.ok()
						.cacheControl(cacheControl)
		                .contentType(MediaType.APPLICATION_OCTET_STREAM)
		                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + supaRelease.getPath() + "\"")
		                .body(supaRelease.getFile());
			}

			// Throw exception if this point is reached
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_DOWNLOAD", "Missing or invalid SUPA release", RequestFailureReason.BAD_REQUEST));
		} catch (SupaReleaseException sre) {

			logger.error("Failed to retrieve specified SUPA release [{}]: {}", ControllerUtils.sanitizeString(releaseVersion), sre.getMessage(), sre);
			return new ResponseEntity<>(sre.getError(), ControllerUtils.translateRequestFailureReasonToHttpErrorCode(sre.getError().getFailureReason()));
		}
	}

	@GetMapping(value = "/getWarRelease/{version:.+}")
	public ResponseEntity<Object> getWarRelease(@RequestHeader("Authorization") String authToken,
											 @PathVariable("version") String releaseVersion,
											 HttpServletResponse response) {

		CacheControl cacheControl = CacheControl.maxAge(0, TimeUnit.SECONDS);

		logger.info("get War release: {}", releaseVersion);
		SupaRelease supaRelease = null;
		try {
			supaRelease = supaReleaseMgmtService.getWarRelease(authToken, releaseVersion);
			if (supaRelease != null && supaRelease.getFile() != null && supaRelease.getFile().length > 0) {
				return ResponseEntity.ok()
						.cacheControl(cacheControl)
						.contentType(MediaType.APPLICATION_OCTET_STREAM)
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + supaRelease.getPath() + "\"")
						.body(supaRelease.getFile());
			}

			// Throw exception if this point is reached
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_DOWNLOAD", "Missing or invalid WAR release", RequestFailureReason.BAD_REQUEST));
		} catch (SupaReleaseException sre) {

			logger.error("Failed to retrieve specified WAR release [{}]: {}", ControllerUtils.sanitizeString(releaseVersion), sre.getMessage(), sre);
			return new ResponseEntity<>(sre.getError(), ControllerUtils.translateRequestFailureReasonToHttpErrorCode(sre.getError().getFailureReason()));
		}
	}

}
