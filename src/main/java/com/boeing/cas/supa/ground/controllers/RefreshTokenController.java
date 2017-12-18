package com.boeing.cas.supa.ground.controllers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.boeing.cas.supa.ground.helpers.AzureADClientHelper;
import com.boeing.cas.supa.ground.helpers.HttpClientHelper;
import com.boeing.cas.supa.ground.pojos.Error;
import com.boeing.cas.supa.ground.pojos.RefreshTokenInput;
import com.boeing.cas.supa.ground.pojos.RefreshTokenOutput;

@RestController
@RequestMapping("/refresh")
public class RefreshTokenController {
	private static String tenantId = "fdacustomertest.onmicrosoft.com";
	private static String clientId = "95d69a21-369b-46cc-aa1d-0b67a2353f59";
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@RequestMapping(method = {RequestMethod.POST })
	public ResponseEntity<Object> getRefreshToken(@RequestBody RefreshTokenInput rft) throws IOException{
		if (isValid(rft)) {
			logger.info("it is valid");
			Object obj = getToken(rft.getRefreshToken());
			if(obj != null){
				if(obj instanceof Error){
					return new ResponseEntity<>((Error) obj, HttpStatus.BAD_REQUEST);
				}else{
					return new ResponseEntity<>((RefreshTokenOutput) obj, HttpStatus.OK);
				}
			}
		}else{
			return new ResponseEntity<>("Not a valid haha", HttpStatus.BAD_REQUEST);
		}
		return null;
	}
	private boolean isValid(RefreshTokenInput rft) {
		return rft != null
		        && rft.getRefreshToken() != null;
	}
	 public static Object getToken(String refreshToken) throws IOException {

	        String encoding = "UTF-8";
	        String params = "client_id=" + clientId + "&refresh_token=" + refreshToken
	                + "&grant_type=refresh_token&resource=https%3A%2F%2Fgraph.windows.net";
	        String path = "https://login.microsoftonline.com/" + tenantId + "/oauth2/token";
	        byte[] data = params.getBytes(encoding);
	        URL url = new URL(path);
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        conn.setRequestMethod("POST");
	        conn.setDoOutput(true);
	        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	        conn.setRequestProperty("Content-Length", String.valueOf(data.length));
	        conn.setConnectTimeout(5 * 1000);
	        OutputStream outStream = conn.getOutputStream();
	        outStream.write(data);
	        outStream.flush();
	        outStream.close();
	        String responseString = "";
	        if (conn.getResponseCode() != 200) {
	        	responseString = HttpClientHelper.getResponseStringFromConn(conn, false);
	        	Error error = AzureADClientHelper.getLoginErrorFromString(responseString);
	        	return error;
	        } else {
	        	responseString = HttpClientHelper.getResponseStringFromConn(conn, true);
	        	RefreshTokenOutput rto = AzureADClientHelper.getRefreshTokenOutputFromString(responseString);
	        	return rto;
	        }
	    }

}
