package com.boeing.cas.supa.ground.controllers;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import com.boeing.cas.supa.ground.utils.KeyVaultProperties;
import com.boeing.cas.supa.ground.utils.KeyVaultRetriever;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.AuthenticationResult;

@RestController
@RequestMapping("/login")
@EnableConfigurationProperties(KeyVaultProperties.class)
public class LoginController {
	
	@Autowired
    private KeyVaultProperties keyVaultProperties;
	//https://stackoverflow.com/questions/45694705/adal4j-java-use-refresh-token-with-username-and-password-to-get-the-access-tok
//	private final static String AUTHORITY = "https://login.microsoftonline.com/fdacustomertest.onmicrosoft.com/";
//    private final static String CLIENT_ID = "95d69a21-369b-46cc-aa1d-0b67a2353f59";
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@RequestMapping(method = {RequestMethod.POST })
	public ResponseEntity<Object> getAccessToken(@RequestBody Credential cred){
		KeyVaultRetriever kvr = new KeyVaultRetriever(keyVaultProperties.getClientId(), keyVaultProperties.getClientKey());
		
		if (isValid(cred)) {
			logger.info("it is valid");
			
			Object ar = getAccessTokenFromUserCredentials(cred.getAzUsername(), cred.getAzPassword(), kvr.getSecretByKey("azure-ad-authority"), kvr.getSecretByKey("azure-ad-clientid"));
			if(ar != null){
				if(ar instanceof AuthenticationResult){
					String getPfxEncodedAsBase64 = kvr.getSecretByKey("client2base64");
					AccessToken at = new AccessToken((AuthenticationResult) ar, getPfxEncodedAsBase64);
					return new ResponseEntity<>(at, HttpStatus.OK);	
				}
				if(ExceptionUtils.indexOfThrowable((Throwable) ar, AuthenticationException.class) >= 0){
					Error error = AzureADClientHelper.getLoginErrorFromString(((AuthenticationException) ar).getMessage());
					
					return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
				}
			}		
		}//https://stackoverflow.com/questions/31971673/how-can-i-get-a-pem-base-64-from-a-pfx-in-java
		else {
			return new ResponseEntity<>("Not a valid request", HttpStatus.BAD_REQUEST);
		}
		logger.info("login called");
		return null;
	}

	private boolean isValid(Credential cred) {
		return cred != null
		        && cred.getAzUsername() != null
		        && cred.getAzPassword() != null;
	}
	private static Object getAccessTokenFromUserCredentials(
            String username, String password, String authority, String clientid) {
        AuthenticationContext context = null;
        AuthenticationResult result = null;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);
            context = new AuthenticationContext(authority, false, service);
            Future<AuthenticationResult> future = context.acquireToken(
                    "https://graph.windows.net", clientid, username, password,
                    null);
            result = future.get();
        } catch (MalformedURLException e) {
        	System.out.println("MalformedURLException");
        	System.out.println(e.getMessage());
			//e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println("InterruptedException");
			System.out.println(e.getMessage());

		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if(cause != null) {
				return cause;
			}
		} catch (AuthenticationException e){
			System.out.println("AuthenticationException");
			System.out.println(e.getMessage());
		} finally {
            service.shutdown();
        }
        return result;
    }
	


}
