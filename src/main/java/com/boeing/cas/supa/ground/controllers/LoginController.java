package com.boeing.cas.supa.ground.controllers;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.boeing.cas.supa.ground.pojos.Credential;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.AuthenticationException;

@RestController
@RequestMapping("/login")
public class LoginController {
	//https://stackoverflow.com/questions/45694705/adal4j-java-use-refresh-token-with-username-and-password-to-get-the-access-tok
	private final static String AUTHORITY = "https://login.microsoftonline.com/fdacustomertest.onmicrosoft.com/";
    private final static String CLIENT_ID = "95d69a21-369b-46cc-aa1d-0b67a2353f59";
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@RequestMapping(method = {RequestMethod.POST })
	public ResponseEntity<Object> uploadFile(@RequestBody Credential cred){
		if (isValid(cred)) {
			logger.info("it is valid");
			AuthenticationResult ar = getAccessTokenFromUserCredentials(cred.getAzUsername(), cred.getAzPassword());
			String accessToken = ar.getAccessToken();
			String refreshToken = ar.getRefreshToken();
			String tokenType = ar.getAccessTokenType();
			System.out.println(accessToken);
			System.out.println(refreshToken);
			System.out.println(tokenType);
			
			
		}else {
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
	private static AuthenticationResult getAccessTokenFromUserCredentials(
            String username, String password) {
        AuthenticationContext context = null;
        AuthenticationResult result = null;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);
            context = new AuthenticationContext(AUTHORITY, false, service);
            Future<AuthenticationResult> future = context.acquireToken(
                    "https://graph.windows.net", CLIENT_ID, username, password,
                    null);
            result = future.get();
            System.out.println(context.toString());
        } catch (MalformedURLException e) {
        	System.out.println("MalformedURLException");
        	System.out.println(e.getMessage());
			//e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println("InterruptedException");
			System.out.println(e.getMessage());

		} catch (ExecutionException e) {
			Throwable cause = e;
			while(cause.getCause() != null) {
			    cause = cause.getCause();
			    System.out.println("cause: "+cause.getMessage());
			}
			System.out.println("ExecutionException");

			System.out.println(e.getMessage());
		} catch (AuthenticationException e){
			System.out.println("AuthenticationException");
			System.out.println(e.getMessage());
		} finally {
            service.shutdown();
        }
        return result;
    }
	

}
