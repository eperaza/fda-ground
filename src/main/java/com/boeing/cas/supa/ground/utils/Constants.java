package com.boeing.cas.supa.ground.utils;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class Constants {

	public static final String AZURE_API_VERSION_PREFIX = "api-version=";

	public static final String ACCEPT_CT_JSON_ODATAMINIMAL = "application/json;odata=minimalmetadata";

	public static final String AUTH_HEADER_PREFIX = "Bearer ";

	public static final String HTML_LINE_BREAK = "<br />";

	public static final String SUCCESS = "Success";

	public static final String FAILURE = "Fail";

	public static final String AAD_GROUP_AIRLINE_PREFIX = "airline-";

	public static final String AAD_GROUP_USER_ROLE_PREFIX = "role-";

	public static final DateTimeFormatter FlightRecordDateTimeFormatterForParse = DateTimeFormatter.ofPattern("uuuuMMdd_HHmmssX");

	public static final DateTimeFormatter DateTimeOffsetFormatterForParse = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss"))
            .optionalStart()
            .appendPattern(".SSSSSSS")
            .optionalEnd()
            .optionalStart()
            .appendPattern(" ")
            .optionalEnd()
            .optionalStart()
            .appendPattern("XXX")
            .optionalEnd()
            .toFormatter();

	public static final DateTimeFormatter FlightRecordDateTimeFormatterForFormat = DateTimeFormatter.ofPattern("yyyyMM");

	public static enum PermissionType {
		DELEGATED, APPLICATION, IMPERSONATION
	}

	public static enum UserAccountState {
		PENDING_USER_ACTIVATION, USER_ACTIVATED, PENDING_DEVICE_ACTIVATION, DEVICE_ACTIVATED
	}

	public static enum RequestFailureReason {
		INTERNAL_SERVER_ERROR, BAD_REQUEST, NOT_FOUND, UNAUTHORIZED, CONFLICT
	}
}
