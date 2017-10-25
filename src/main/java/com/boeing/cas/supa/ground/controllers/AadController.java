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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.boeing.cas.supa.ground.helpers.AzureADAuthHelper;
import com.boeing.cas.supa.ground.helpers.EmailHelper;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.utils.MicrosoftGraphUtil;
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
			result = (AuthenticationResult) session.getAttribute(AzureADAuthHelper.PRINCIPAL_SESSION_NAME);

			if (result == null) {
				logger.error("result is null");
				// model.addAttribute("error", new
				// Exception("AuthenticationResult not found in session."));
			} else {
				User user;
				try {
					String tenant = session.getServletContext().getInitParameter("tenant");
					String accessToken = result.getAccessToken();
					MicrosoftGraphUtil mgu = new MicrosoftGraphUtil(tenant, accessToken);
					user = mgu.getUsernamesFromGraph(result.getUserInfo().getUniqueId());
					List<String> userEmails = user.getOtherMails();
					boolean send = EmailHelper.sendEmail(null, null, null);

					// data = getUsernamesFromGraph(result.getAccessToken(),
					// tenant, result.getUserInfo().getUniqueId());
					// model.addAttribute("tenant", tenant);
					// model.addAttribute("users", data);
					// model.addAttribute("userInfo", result.getUserInfo());
				} catch (Exception e) {
					logger.error(e.getMessage());
					// model.addAttribute("error", e);

				}
			}
			response = new ResponseEntity<Object>("hahaa - ", HttpStatus.OK);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			response = new ResponseEntity<Object>("you suck!! - ", HttpStatus.OK);
		}
		return response;
	}

	@RequestMapping(value = "/testAuth", method = { RequestMethod.GET })
	public ResponseEntity<Map<String, Object>> getGreeting() {
		Map<String, Object> responseMap = new HashMap<>();
		responseMap.put("greeting", "hello world");
		return new ResponseEntity<Map<String, Object>>(responseMap, HttpStatus.OK);
	}

	@RequestMapping(value = "/testAuthHelp", method = { RequestMethod.GET })
	public ResponseEntity<Map<String, Object>> getAuthHelp(HttpServletRequest httpRequest) {
		Enumeration<String> headerNames = httpRequest.getHeaderNames();
		while(headerNames.hasMoreElements()){
			String headername = headerNames.nextElement();
			Enumeration<String> headerValues = httpRequest.getHeaders(headername);
			while(headerValues.hasMoreElements()){
				String headerV = headerValues.nextElement();
				logger.info(headername + "=>" + headerV);
			}
		}
		HttpSession session = httpRequest.getSession();
		AuthenticationResult result = (AuthenticationResult) session
				.getAttribute(AzureADAuthHelper.PRINCIPAL_SESSION_NAME);
		if (result == null) {
			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("You are not authorized", "Authorization has been denied for this request");
			return new ResponseEntity<Map<String, Object>>(responseMap, HttpStatus.UNAUTHORIZED);
		} else {
			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("greeting", "hello world");
			return new ResponseEntity<Map<String, Object>>(responseMap, HttpStatus.OK);
		}

	}

}
