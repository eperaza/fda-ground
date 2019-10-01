package com.boeing.cas.supa.ground.controllers;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import com.boeing.cas.supa.ground.exceptions.FeatureManagementException;
import com.boeing.cas.supa.ground.services.FeatureManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.boeing.cas.supa.ground.exceptions.FileDownloadException;
import com.boeing.cas.supa.ground.services.FileManagementService;
import com.boeing.cas.supa.ground.utils.ControllerUtils;

@Controller
public class FileDownloadController {

	private final static String PREFERENCES_STORAGE_CONTAINER = "preferences";
	private final static String PREFERENCES_REFRESH_STORAGE_CONTAINER = "preferencesRefresh";

	@Autowired
	private FileManagementService fileManagementService;

	@Autowired
	private FeatureManagementService featureManagementService;


	@RequestMapping(value = "/download", method = RequestMethod.GET)
	public ResponseEntity<Object> getFileFromStorage(@RequestHeader("Authorization") String authToken,
			@RequestParam String file, @RequestParam String type,
			HttpServletResponse response) {

		CacheControl cacheControl = CacheControl.maxAge(0, TimeUnit.SECONDS);

		try {

			if (PREFERENCES_STORAGE_CONTAINER.equals(type) || PREFERENCES_REFRESH_STORAGE_CONTAINER.equals(type)) {

				//note: get Preferences from SQL - do NOT need file information.
				byte[] fileInBytes = featureManagementService.getAllPreferencesFromSQL(authToken,
					PREFERENCES_STORAGE_CONTAINER.equals(type)? true: false);
				return ResponseEntity.ok()
						.cacheControl(cacheControl)
						.body(fileInBytes);
			} else {

				byte[] fileInBytes = this.fileManagementService.getFileFromStorage(file, type, authToken);
				return ResponseEntity.ok()
						.cacheControl(cacheControl)
						.body(fileInBytes);
			}
		} catch (FeatureManagementException fme) {
			return new ResponseEntity<>(fme.getError(), ControllerUtils.translateRequestFailureReasonToHttpErrorCode(fme.getError().getFailureReason()));
		} catch (FileDownloadException fde) {
			return new ResponseEntity<>(fde.getError(), ControllerUtils.translateRequestFailureReasonToHttpErrorCode(fde.getError().getFailureReason()));
		}
	}
}
