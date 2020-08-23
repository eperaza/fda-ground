package com.boeing.cas.supa.ground.utils;

import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ControllerUtils {


	private static Logger logger = LoggerFactory.getLogger(ControllerUtils.class);

	public static HttpStatus translateRequestFailureReasonToHttpErrorCode(RequestFailureReason failureReason) {

		switch (failureReason) {

			case BAD_REQUEST:
				return HttpStatus.BAD_REQUEST;
			case CONFLICT:
				return HttpStatus.CONFLICT;
			case UNAUTHORIZED:
				return HttpStatus.UNAUTHORIZED;
			case NOT_FOUND:
				return HttpStatus.NOT_FOUND;
			case ALREADY_REPORTED:
				return HttpStatus.ALREADY_REPORTED;
			default:
		}

		return HttpStatus.INTERNAL_SERVER_ERROR;
	}

	public static RequestFailureReason translateHttpErrorCodeToRequestFailureReason(int responseCode) {

		switch (responseCode) {

			case 208:
				return RequestFailureReason.ALREADY_REPORTED;
			case 400:
				return RequestFailureReason.BAD_REQUEST;
			case 409:
				return RequestFailureReason.CONFLICT;
			case 401:
				return RequestFailureReason.UNAUTHORIZED;
			case 404:
				return RequestFailureReason.NOT_FOUND;
			default:
		}

		return RequestFailureReason.INTERNAL_SERVER_ERROR;
	}

	public static <T> T fromJSON(final TypeReference<T> type, final String jsonPacket) {
		T data = null;

		try {
			data = new ObjectMapper().readValue(jsonPacket, type);
		} catch (Exception e) {
			// Handle the problem
		}
		return data;
	}

	public static String saveUploadedFiles(List<MultipartFile> files) throws IOException {

		Path tempDirPath = Files.createTempDirectory(StringUtils.EMPTY);
		String uploadFolder = tempDirPath.toString();

		for (MultipartFile file : files) {

			if (file.isEmpty()) {
				continue; // skip to next iteration
			}

			byte[] bytes = file.getBytes();
			Path path = Paths.get(uploadFolder + File.separator + file.getOriginalFilename());
			Files.write(path, bytes);
		}

		return uploadFolder;
	}

	public static String saveUploadedZip(byte[] zipFile, String fileName) throws IOException {
		Path tempDirPath = Files.createTempDirectory(StringUtils.EMPTY);
		String uploadFolder = tempDirPath.toString();
		Path path = Paths.get(uploadFolder + File.separator + fileName);

		logger.debug("saveUploadedZip path: " + path.toString());

		Files.write(path, zipFile);
		return uploadFolder;
	}


	public static String sanitizeString(String inputStr) {

		return !StringUtils.isEmpty(inputStr) ? HtmlUtils.htmlEscape(inputStr.replaceAll("[\\r\\n]", "_"))
				: "";
	}

}
