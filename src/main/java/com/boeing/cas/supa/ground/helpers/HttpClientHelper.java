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
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.boeing.cas.supa.ground.pojos.Group;
import com.boeing.cas.supa.ground.pojos.User;
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

    private HttpClientHelper() {
        super();
    }

    public static String getResponseStringFromConn(HttpURLConnection conn, boolean isSuccess) throws IOException {

        BufferedReader reader = null;
        if (isSuccess) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }
        StringBuilder stringBuilder = new StringBuilder();
        String line = "";
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }

        return stringBuilder.toString();
    }
    
    public static User getUserInfoFromHeader(HttpServletRequest httpRequest){
    	HeaderMapRequestWrapper header = new HeaderMapRequestWrapper(httpRequest);
		String tokenName = header.getHeader("authorization");
		String uniqueId = getUniqueIdFromJWT(tokenName);
		MicrosoftGraphUtil mgu = new MicrosoftGraphUtil("fdacustomertest.onmicrosoft.com", tokenName.replaceFirst("Bearer ", ""));
		User user = mgu.getUsernamesFromGraph(uniqueId);
		ArrayList<Group> group = mgu.getGroupFromGraph(uniqueId);
		user.setGroups(group);
    	return user;
    }
    public static User getUserInfoFromAuthToken(String tokenName){
    	String uniqueId = getUniqueIdFromJWT(tokenName);
		MicrosoftGraphUtil mgu = new MicrosoftGraphUtil("fdacustomertest.onmicrosoft.com", tokenName.replaceFirst("Bearer ", ""));
		User user = mgu.getUsernamesFromGraph(uniqueId);
		ArrayList<Group> group = mgu.getGroupFromGraph(uniqueId);
		user.setGroups(group);
    	return user;
    }
    private static String getUniqueIdFromJWT(String xAuth) {
		String uniqueId = null;
		if(xAuth.contains("Bearer")){
    		xAuth = xAuth.replaceFirst("Bearer ", "");
    		Map<String, Object> claimsMap;
			try {
				claimsMap = JWTParser.parse(xAuth).getJWTClaimsSet().getClaims();
				uniqueId = (String) claimsMap.get("oid");
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
		}
		return uniqueId;
	}
}
