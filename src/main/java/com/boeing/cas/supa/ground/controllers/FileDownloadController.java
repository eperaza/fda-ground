package com.boeing.cas.supa.ground.controllers;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

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

	@Autowired
	private FileManagementService fileManagementService;

	@RequestMapping(value = "/download", method = RequestMethod.GET)
	public ResponseEntity<Object> getFileFromStorage(@RequestHeader("Authorization") String authToken,
			@RequestParam String file, @RequestParam String type,
			HttpServletResponse response) {

		CacheControl cacheControl = CacheControl.maxAge(0, TimeUnit.SECONDS);

		try {
			byte[] fileInBytes = this.fileManagementService.getFileFromStorage(file, type, authToken);
			return ResponseEntity.ok()
					.cacheControl(cacheControl)
					.body(fileInBytes);
		} catch (FileDownloadException fde) {
			return new ResponseEntity<>(fde.getError(), ControllerUtils.translateRequestFailureReasonToHttpErrorCode(fde.getError().getFailureReason()));
		}
	}
}
