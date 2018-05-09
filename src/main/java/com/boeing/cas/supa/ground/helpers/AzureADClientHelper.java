package com.boeing.cas.supa.ground.helpers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boeing.cas.supa.ground.pojos.Error;
import com.boeing.cas.supa.ground.pojos.RefreshTokenOutput;
import com.boeing.cas.supa.ground.utils.Constants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.nimbusds.jwt.JWTParser;

public class AzureADClientHelper {

    private static final Logger logger = LoggerFactory.getLogger(AzureADClientHelper.class);

    // Hide default constructor
    private AzureADClientHelper() {}
    
	public static Error getLoginErrorFromString(String responseString) {

		Error errorObj = null;
		try {

			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> map = mapper.readValue(responseString, new TypeReference<HashMap<String, String>>() {});
			String errorDesc = map.get("error_description");
			String errorString = errorDesc.split("\\r?\\n")[0];

			errorObj = new Error(map.get("error"), errorString);
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
	
	public static Object getAccessTokenFromUserCredentials(String username, String password, String authority, String clientId) {

        AuthenticationResult result = null;

        ExecutorService service = null;
        try {

        	service = Executors.newFixedThreadPool(1);
        	AuthenticationContext context = new AuthenticationContext(authority, false, service);
            Future<AuthenticationResult> future = context.acquireToken("https://graph.windows.net", clientId, username, password, null);
            result = future.get();
        } catch (MalformedURLException murle) {
        	logger.error("MalformedURLException: {}", murle.getMessage(), murle);
		} catch (InterruptedException ie) {
        	logger.error("InterruptedException: {}", ie.getMessage(), ie);
        	Thread.currentThread().interrupt();
		} catch (ExecutionException ee) {
			Throwable cause = ee.getCause();
			if (cause != null) {
	        	logger.error("ExecutionException cause: {}", cause.getMessage(), ee);
			}
		} catch (AuthenticationException ae) {
        	logger.error("AuthenticationException: {}", ae.getMessage(), ae);
		} finally {
            if (service != null) { service.shutdown(); }
        }

        return result;
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

