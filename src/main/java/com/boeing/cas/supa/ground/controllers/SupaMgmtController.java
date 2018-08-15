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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boeing.cas.supa.ground.exceptions.SupaReleaseException;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.SupaRelease;
import com.boeing.cas.supa.ground.services.SupaReleaseManagementService;
import com.boeing.cas.supa.ground.utils.ControllerUtils;

@RestController
@RequestMapping(path="/supa-release-mgmt")
public class SupaMgmtController {

	private final Logger logger = LoggerFactory.getLogger(SupaMgmtController.class);

	@Autowired
	private SupaReleaseManagementService supaReleaseMgmtService;

	@RequestMapping(path="/list", method = { RequestMethod.GET }, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> listSupaReleases(@RequestHeader("Authorization") String authToken, @RequestParam short versions) {

		CacheControl cacheControl = CacheControl.maxAge(0, TimeUnit.SECONDS);

		try {
			List<SupaRelease> supaReleases = this.supaReleaseMgmtService.listSupaReleases(authToken, versions);
			logger.debug("{} release(s)", CollectionUtils.isEmpty(supaReleases) ? 0 : supaReleases.size());
			return ResponseEntity.ok()
					.cacheControl(cacheControl)
					.contentType(MediaType.APPLICATION_JSON_UTF8)
					.body(supaReleases);
		} catch (SupaReleaseException sre) {
			logger.error("Failed to retrieve list of SUPA releases: {}", sre.getMessage(), sre);
			return new ResponseEntity<>(new ApiError("SUPA_RELEASE_LIST", sre.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
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
			throw new SupaReleaseException("Missing or invalid SUPA release");
		} catch (SupaReleaseException sre) {

			logger.error("Failed to retrieve specified SUPA release [{}]: {}", ControllerUtils.sanitizeString(releaseVersion), sre.getMessage(), sre);
			return new ResponseEntity<>(new ApiError("SUPA_RELEASE_DOWNLOAD", sre.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
