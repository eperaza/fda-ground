package com.boeing.cas.supa.ground.controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.boeing.cas.supa.ground.utils.ControllerUtils;

@Controller
public class FileDownloadController {

	@Autowired
	private Map<String, String> appProps;

	private final Logger logger = LoggerFactory.getLogger(FileDownloadController.class);

	@ResponseBody
	@RequestMapping(value = "/download", method = RequestMethod.GET)
	public byte[] downloadJSONFile(@RequestParam("file") String file, @RequestParam("type") String type,
			HttpServletRequest request, HttpServletResponse response) {

		try {

			AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"), this.appProps.get("StorageKey"));
			if (file != null) {

				ByteArrayOutputStream outputStream = null;
				// Files are always case sensitive so no need to change fileparam...PLEASE
				outputStream = asu.downloadFile(type.toLowerCase(), file);
				if (outputStream != null) {
					response.setStatus(HttpServletResponse.SC_OK);
					return outputStream.toByteArray();
				}

				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return new byte[0];
			}
		} catch (IOException e) {
			logger.error("ApiError retrieving file [{}] of type [{}]: {}", ControllerUtils.sanitizeString(file),
					ControllerUtils.sanitizeString(type), e.getMessage(), e);
		}

		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		return new byte[0];
	}
}
