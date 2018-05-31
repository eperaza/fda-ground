/*******************************************************************************
 * Copyright © Microsoft Open Technologies, Inc.
 * 
 * All Rights Reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
 * ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
 * PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
 * 
 * See the Apache License, Version 2.0 for the specific language
 * governing permissions and limitations under the License.
 ******************************************************************************/
package com.boeing.cas.supa.ground.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boeing.cas.supa.ground.pojos.Group;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.HeaderMapRequestWrapper;
import com.boeing.cas.supa.ground.utils.MicrosoftGraphUtil;
import com.nimbusds.jwt.JWTParser;

/**
 * This is Helper class for all RestClient class.
 * 
 * @author Azure Active Directory Contributor
 * 
 */
public class HttpClientHelper {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientHelper.class);

    // Hide default constructor
    private HttpClientHelper() {}

    public static String getResponseStringFromConn(HttpURLConnection conn, boolean isSuccess) throws IOException {

        StringBuilder stringBuilder = new StringBuilder();

    	try (BufferedReader reader = isSuccess
    			? new BufferedReader(new InputStreamReader(conn.getInputStream()))
    			: new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
    		
            String line = null;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
    	}

        return stringBuilder.toString();
    }
    
    public static User getUserInfoFromHeader(String tenantName, HttpServletRequest httpRequest) {

    	HeaderMapRequestWrapper header = new HeaderMapRequestWrapper(httpRequest);
		String tokenName = header.getHeader("authorization");
		String uniqueId = HttpClientHelper.getUniqueIdFromJWT(tokenName);
		MicrosoftGraphUtil mgu = new MicrosoftGraphUtil(tenantName, tokenName.replaceFirst(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY));
		User user = mgu.getUsernamesFromGraph(uniqueId);
		List<Group> group = mgu.getGroupFromGraph(uniqueId);
		user.setGroups(group);

		return user;
    }

    public static User getUserInfoFromAuthToken(String tenantName, String tokenName) {

    	String uniqueId = HttpClientHelper.getUniqueIdFromJWT(tokenName);
		MicrosoftGraphUtil mgu = new MicrosoftGraphUtil(tenantName, tokenName.replaceFirst(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY));
		User user = mgu.getUsernamesFromGraph(uniqueId);
		List<Group> group = mgu.getGroupFromGraph(uniqueId);
		user.setGroups(group);

		return user;
    }

    private static String getUniqueIdFromJWT(String xAuth) {

    	String uniqueId = null;
		if (xAuth.contains(Constants.AUTH_HEADER_PREFIX.trim())) {

            try {
                Map<String, Object> claimsMap = JWTParser.parse(xAuth.replaceFirst(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY)).getJWTClaimsSet().getClaims();
                uniqueId = (String) claimsMap.get("oid");
            }
            catch (ParseException pe) {
                logger.error("Failed to extract claims and/or unique ID from authentication header: {}", pe.getMessage(), pe);
            }
		}

		return uniqueId;
	}
}
