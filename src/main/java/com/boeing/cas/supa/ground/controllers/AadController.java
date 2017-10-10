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
package com.boeing.cas.supa.ground.controllers;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.boeing.cas.supa.ground.helpers.AzureADAuthHelper;
import com.boeing.cas.supa.ground.helpers.HttpClientHelper;
import com.boeing.cas.supa.ground.helpers.JSONHelper;
import com.boeing.cas.supa.ground.pojos.User;
import com.microsoft.aad.adal4j.AuthenticationResult;

@RestController
@RequestMapping("/secure/aad")
public class AadController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@RequestMapping(method = { RequestMethod.GET, RequestMethod.POST })
	public ResponseEntity<Object> getDirectoryObjects(HttpServletRequest httpRequest) {
		logger.info("get directory objects");
		HttpSession session = httpRequest.getSession();
		logger.info("obtained session");
		AuthenticationResult result = null;
		logger.info("obtained authentication result");
		ResponseEntity<Object> response = null;
        try {
        	result = (AuthenticationResult) session
    				.getAttribute(AzureADAuthHelper.PRINCIPAL_SESSION_NAME);
        	
        	if (result == null) {
    			//model.addAttribute("error", new Exception("AuthenticationResult not found in session."));
        	}
        	else {
    			List<User> data;
    			try {
    				String tenant = session.getServletContext().getInitParameter("tenant");
    				String accessToken = result.getAccessToken();
    				System.out.println("tenant: " + tenant);
    				System.out.println("accessToken: "+accessToken);
    				data = getUsernamesFromGraph(result.getAccessToken(), tenant, result.getUserInfo().getUniqueId());
//    				model.addAttribute("tenant", tenant);
//    				model.addAttribute("users", data);
//    				model.addAttribute("userInfo", result.getUserInfo());
    			}
    			catch (Exception e) {
    				//model.addAttribute("error", e);
    			}
        	}
            response = new ResponseEntity<Object>("hahaa - ", HttpStatus.OK);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response = new ResponseEntity<Object>("you suck!! - ", HttpStatus.OK);
        }
        return response;
	}

	private List<User> getUsernamesFromGraph(String accessToken, String tenant, String uniqueID) throws Exception {

		logger.info("get user names from graph");
		logger.info("Tenant " + tenant);
		logger.info("Access token: " + accessToken);

		URL url = new URL(
				String.format("https://graph.windows.net/%s/users/%s?api-version=2013-04-05", tenant, uniqueID, accessToken));
		logger.info("URL: " + url.toString());

		HttpURLConnection conn = null;
		JSONArray users = new JSONArray();
		try {
			conn = (HttpURLConnection) url.openConnection();
			// Set the appropriate header fields in the request header.
			conn.setRequestProperty("api-version", "2013-04-05");
			conn.setRequestProperty("Authorization", accessToken);
			conn.setRequestProperty("Accept", "application/json;");
			String goodRespStr = HttpClientHelper.getResponseStringFromConn(conn, true);
			logger.info("goodRespStr ->" + goodRespStr);
			int responseCode = conn.getResponseCode();
			JSONObject response = HttpClientHelper.processGoodRespStr(responseCode, goodRespStr);
			users = JSONHelper.fetchDirectoryObjectJSONArray(response);
		} catch (Exception e) {
			logger.error("EXCEPTION!!! => " + e.getMessage(), e);
		}

		List<User> userObjects = new ArrayList<>();
		User user;
		for (int i = 0; i < users.length(); i++) {
			JSONObject thisUserJSONObject = users.optJSONObject(i);
			user = new User();
			JSONHelper.convertJSONObjectToDirectoryObject(thisUserJSONObject, user);
			userObjects.add(user);
		}
		return userObjects;
	}

	@RequestMapping(value = "/testAuth", method = { RequestMethod.GET })
	public ResponseEntity<Map<String, Object>> getGreeting() {
		
		Map<String, Object> responseMap = new HashMap<>();
		responseMap.put("greeting", "hello world");
		return new ResponseEntity<Map<String, Object>>(responseMap, HttpStatus.OK);
	}
	
}
