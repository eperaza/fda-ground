package com.boeing.cas.supa.ground.helpers;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.RefreshTokenOutput;
import com.boeing.cas.supa.ground.utils.Constants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTParser;

public class AzureADClientHelper {

    private static final Logger logger = LoggerFactory.getLogger(AzureADClientHelper.class);

    // Hide default constructor
    private AzureADClientHelper() {}
    
	public static ApiError getLoginErrorFromString(String responseString) {

		ApiError errorObj = null;
		try {

			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> map = mapper.readValue(responseString, new TypeReference<HashMap<String, String>>() {});
			String errorDesc = map.get("error_description");
			String errorString = errorDesc.split("\\r?\\n")[0];

			errorObj = new ApiError(map.get("error"), errorString);
		} catch (IOException ioe) {
			logger.error("Failed to extract login error from string: {}", ioe.getMessage(), ioe);
		}
		
		return errorObj;
	}

	public static RefreshTokenOutput getRefreshTokenOutputFromString(String responseString) {

		RefreshTokenOutput refreshTokenOutput = null;
		try {

			ObjectMapper mapper = new ObjectMapper(); 
			Map<String, String> map = mapper.readValue(responseString, new TypeReference<HashMap<String, String>>() {});
			String tokenType = map.get("token_type");
			String expiresIn = map.get("expires_in");
			String expiresOn = map.get("expires_on");
			String notBefore = map.get("not_before");
			String accessToken = map.get("access_token");
			String refreshToken = map.get("refresh_token");

			refreshTokenOutput = new RefreshTokenOutput(tokenType, expiresIn, expiresOn, notBefore, accessToken, refreshToken);
		} catch (IOException ioe) {
			logger.error("Failed to extract refresh token output from string: {}", ioe.getMessage(), ioe);
		}

		return refreshTokenOutput;
	}

	public static String getUniqueIdFromJWT(String xAuth) {

		String uniqueId = null;
		try {
			Map<String, Object> claimsMap = JWTParser
					.parse(xAuth.contains(Constants.AUTH_HEADER_PREFIX.trim()) ? xAuth.replaceFirst(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY) : xAuth).getJWTClaimsSet()
					.getClaims();
			uniqueId = (String) claimsMap.get("oid");
		} catch (ParseException pe) {
			logger.error("Failed to extract claims and/or unique ID from authentication header: {}",
					pe.getMessage(), pe);
		}

		return uniqueId;
	}
}
