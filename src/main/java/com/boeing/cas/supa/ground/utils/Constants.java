package com.boeing.cas.supa.ground.utils;

public class Constants {

	public static final String AZURE_API_VERSION_PREFIX = "api-version=";

    public static final String ACCEPT_CT_JSON_ODATAMINIMAL = "application/json;odata=minimalmetadata";
    
    public static final String AUTH_HEADER_PREFIX = "Bearer ";
    
    public static final String HTML_LINE_BREAK = "<br />";
    
	public static enum PermissionType {
		DELEGATED, APPLICATION, IMPERSONATION
	}
}
