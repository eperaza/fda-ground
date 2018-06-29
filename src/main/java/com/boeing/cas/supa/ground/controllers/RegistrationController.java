package com.boeing.cas.supa.ground.controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.boeing.cas.supa.ground.exceptions.UserAccountRegistrationException;
import com.boeing.cas.supa.ground.helpers.AzureADClientHelper;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.Credential;
import com.boeing.cas.supa.ground.pojos.UserAccountActivation;
import com.boeing.cas.supa.ground.pojos.UserRegistration;
import com.boeing.cas.supa.ground.services.AzureADClientService;
import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.AuthenticationResult;

@RestController
public class RegistrationController {

	private final Logger logger = LoggerFactory.getLogger(RegistrationController.class);

	@Autowired
	private Map<String, String> appProps;

	@Autowired
	private AzureADClientService aadClient;

	@RequestMapping(path="/register", method = { RequestMethod.POST })
	public ResponseEntity<Object> getAccessToken(@RequestBody Credential cred) {

		if (cred != null && cred.isValid()) {

			logger.debug("Received properly formed credentials");
			Object ar = aadClient.getAccessTokenFromUserCredentials(cred.getAzUsername(), cred.getAzPassword(), this.appProps.get("AzureADTenantAuthEndpoint"), this.appProps.get("AzureADAppClientID"));

			if (ar == null) {
				return new ResponseEntity<>("Invalid request with improperly formed credentials", HttpStatus.BAD_REQUEST);
			}

			if (ar instanceof AuthenticationResult) {

				AuthenticationResult authResult = (AuthenticationResult) ar;
				// Get user from HttpClientHelper.getUserInfoFromAuthToken(authResult.getAccessTokenType() + " " + authResult.getAccessToken())
				// To get user groups from Token: List<Group> groups = user.getGroups() and then iterate through user's groups
				// Article ref: https://stackoverflow.com/questions/31971673/how-can-i-get-a-pem-base-64-from-a-pfx-in-java
				String getPfxEncodedAsBase64 = this.appProps.get("fdadvisor2base64");
				String getPlistFromBlob = getPlistFromBlob("preferences", "ADW.plist");
				String mobileConfigFromBlob = getPlistFromBlob("config", "supaConfigEFO.mobileconfig");
				if (getPlistFromBlob != null && mobileConfigFromBlob != null) {
					UserRegistration userReg = new UserRegistration(authResult, getPfxEncodedAsBase64, null, null, getPlistFromBlob, mobileConfigFromBlob);
					return new ResponseEntity<>(userReg, HttpStatus.OK);
				}
			}

			if (ExceptionUtils.indexOfThrowable((Throwable) ar, AuthenticationException.class) >= 0) {
				ApiError apiError = AzureADClientHelper.getLoginErrorFromString(((AuthenticationException) ar).getMessage());
				return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
			}
		}

		logger.warn("User registration requested (improper token information received in request)!");
		return null;
	}

	@RequestMapping(path="/registeruser", method = { RequestMethod.POST })
	public ResponseEntity<Object> registerUserAccount(@RequestBody UserAccountActivation userAccountActivation) throws UserAccountRegistrationException {

		Object result = aadClient.enableUserAndSetPassword(userAccountActivation);

		if (result instanceof ApiError) {
			return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

    private String getPlistFromBlob(String containerName, String fileName) {

        String base64 = null;
        try {
            AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"), this.appProps.get("StorageKey"));
            try (ByteArrayOutputStream outputStream = asu.downloadFile(containerName, fileName)) {
                base64 = Base64.getEncoder().encodeToString(outputStream.toString().getBytes());
            }
        }
        catch (IOException ioe) {
            logger.error("Failed to retrieve Plist [{}]: {}", fileName, ioe.getMessage(), ioe);
        }

        return base64;
    }
}
