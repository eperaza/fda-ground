package com.boeing.cas.supa.ground.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.boeing.cas.supa.ground.exceptions.UserAuthenticationException;
import com.boeing.cas.supa.ground.pojos.AccessToken;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.Credential;
import com.boeing.cas.supa.ground.services.AzureADClientService;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

@CrossOrigin
@RestController
@RequestMapping("/login")
public class LoginController {

	@Autowired
	private AzureADClientService aadClient;

	@RequestMapping(method = { RequestMethod.POST })
	public ResponseEntity<AccessToken> getAccessToken(@RequestBody Credential cred) throws UserAuthenticationException {

		if (cred == null || !cred.isValid()) {
			throw new UserAuthenticationException(new ApiError("USER_AUTH_FAILURE", "Request does not contain properly formed credentials", RequestFailureReason.BAD_REQUEST));
		}

		return new ResponseEntity<>(aadClient.loginUserForAccessToken(cred), HttpStatus.OK);
	}
}
