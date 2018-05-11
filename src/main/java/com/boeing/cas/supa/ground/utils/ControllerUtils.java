package com.boeing.cas.supa.ground.utils;

import org.springframework.http.HttpStatus;

import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ControllerUtils {

	public static HttpStatus translateRequestFailureReasonToHttpErrorCode(RequestFailureReason failureReason) {

		switch (failureReason) {

			case BAD_REQUEST:
				return HttpStatus.BAD_REQUEST;
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
}
