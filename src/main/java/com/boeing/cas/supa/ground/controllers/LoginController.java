package com.boeing.cas.supa.ground.controllers;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.boeing.cas.supa.ground.helpers.AzureADClientHelper;
import com.boeing.cas.supa.ground.pojos.AccessToken;
import com.boeing.cas.supa.ground.pojos.Credential;
import com.boeing.cas.supa.ground.pojos.Error;
import com.boeing.cas.supa.ground.pojos.KeyVaultProperties;
import com.boeing.cas.supa.ground.utils.KeyVaultRetriever;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.AuthenticationResult;

// Article ref: https://stackoverflow.com/questions/45694705/adal4j-java-use-refresh-token-with-username-and-password-to-get-the-access-tok

@RestController
@RequestMapping("/login")
@EnableConfigurationProperties(KeyVaultProperties.class)
public class LoginController {

	private final Logger logger = LoggerFactory.getLogger(LoginController.class);

	@Autowired
    private KeyVaultProperties keyVaultProperties;

	@RequestMapping(method = { RequestMethod.POST })
	public ResponseEntity<Object> getAccessToken(@RequestBody Credential cred) {

		KeyVaultRetriever kvr = new KeyVaultRetriever(this.keyVaultProperties.getClientId(), this.keyVaultProperties.getClientKey());
		
		if (cred != null && cred.isValid()) {

			logger.info("Credentials received in proper form");
			Object ar = AzureADClientHelper.getAccessTokenFromUserCredentials(cred.getAzUsername(), cred.getAzPassword(), kvr.getSecretByKey("azure-ad-authority"), kvr.getSecretByKey("azure-ad-clientid"));
			if (ar != null) {

				if (ar instanceof AuthenticationResult) {
					// Article ref: //https://stackoverflow.com/questions/31971673/how-can-i-get-a-pem-base-64-from-a-pfx-in-java
					String getPfxEncodedAsBase64 = kvr.getSecretByKey("client2base64");
					AccessToken at = new AccessToken((AuthenticationResult) ar, getPfxEncodedAsBase64);
					return new ResponseEntity<>(at, HttpStatus.OK);					
				}

				if (ExceptionUtils.indexOfThrowable((Throwable) ar, AuthenticationException.class) >= 0) {
					Error error = AzureADClientHelper.getLoginErrorFromString(((AuthenticationException) ar).getMessage());
					return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
				}
			}
		}
		else {
			return new ResponseEntity<>("Request does not contain properly formed credentials", HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>("Missing or invalid authentication result", HttpStatus.BAD_REQUEST);
	}
}
