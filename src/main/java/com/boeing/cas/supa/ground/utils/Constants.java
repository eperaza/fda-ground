package com.boeing.cas.supa.ground.utils;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.List;

public class Constants {

	public static final String AZURE_API_VERSION_PREFIX = "api-version=";

	public static final String ACCEPT_CT_JSON_ODATAMINIMAL = "application/json;odata=minimalmetadata";

	public static final String ACCEPT_CT_JSON_OVERBOSE = "application/json;odata=verbose";

	public static final String ACCEPT_CT_JSON_ONOMETADATA = "application/json;odata=nometadata";

	public static final String ACCEPT_CT_JSON_ONOTHING = "application/json";

	public static final String AUTH_HEADER_PREFIX = "Bearer ";

	public static final String CHECKSUM_PREFIX_SHA1 = ".sha1";

	public static final String HTML_LINE_BREAK = "<br />";

	public static final String SUCCESS = "Success";

	public static final String FAILURE = "Fail";

	public static final String AAD_GROUP_AIRLINE_PREFIX = "airline-";

	public static final String AAD_GROUP_USER_ROLE_PREFIX = "role-";

	public static final List<String> ALLOWED_USER_ROLES = Arrays.asList(new String[] { "role-airlinefocal", "role-airlinepilot", "role-airlinemaintenance" });

	public static final DateTimeFormatter FlightRecordDateTimeFormatterForParse = DateTimeFormatter.ofPattern("uuuuMMdd_HHmmssX");

	public static final DateTimeFormatter SupaSystemLogDateTimeFormatterForParse = DateTimeFormatter.ofPattern("uuuuMMdd_HHmmssX");

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

	public static final DateTimeFormatter SupaSystemLogDateTimeFormatterForFormat = DateTimeFormatter.ofPattern("yyyyMM");

	public static enum PermissionType {
		DELEGATED, APPLICATION, IMPERSONATION
	}

	public static enum UserAccountState {
		PENDING_USER_ACTIVATION, USER_ACTIVATED, PENDING_DEVICE_ACTIVATION, DEVICE_ACTIVATED
	}

	public static enum RequestFailureReason {
        INTERNAL_SERVER_ERROR, BAD_REQUEST, NOT_FOUND, UNAUTHORIZED, CONFLICT, ALREADY_REPORTED
	}

	public static String PATTERN_EMAIL_REGEX = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
}
