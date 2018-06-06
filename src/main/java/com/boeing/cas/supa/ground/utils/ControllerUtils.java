package com.boeing.cas.supa.ground.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ControllerUtils {

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
			default:
		}

		return HttpStatus.INTERNAL_SERVER_ERROR;
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

	public static String sanitizeString(String inputStr) {

		return !StringUtils.isEmpty(inputStr) ? HtmlUtils.htmlEscape(inputStr.replaceAll("[\\r\\n]", "_"))
				: "";
	}
}
