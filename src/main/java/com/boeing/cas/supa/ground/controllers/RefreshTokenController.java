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

	private final Logger logger = LoggerFactory.getLogger(RefreshTokenController.class);

	private static final String TENANT_ID = "fdacustomertest.onmicrosoft.com";
	private static final String CLIENT_ID = "95d69a21-369b-46cc-aa1d-0b67a2353f59";

	@RequestMapping(method = { RequestMethod.POST })
	public ResponseEntity<Object> getRefreshToken(@RequestBody RefreshTokenInput refreshTokenInput) throws IOException {

		if (isValid(refreshTokenInput)) {

			logger.debug("Request contains refresh token");
			Object obj = RefreshTokenController.getToken(refreshTokenInput.getRefreshToken());
			if (obj != null) {

				if (obj instanceof Error) {
					return new ResponseEntity<>((Error) obj, HttpStatus.BAD_REQUEST);
				}

				return new ResponseEntity<>((RefreshTokenOutput) obj, HttpStatus.OK);
			}
		}

		return new ResponseEntity<>("Request missing refresh token", HttpStatus.BAD_REQUEST);
	}

	private boolean isValid(RefreshTokenInput refreshTokenInput) {
		return refreshTokenInput != null && refreshTokenInput.getRefreshToken() != null;
	}

	public static Object getToken(String refreshToken) throws IOException {

		String encoding = "UTF-8";
		String params = new StringBuilder("client_id=").append(CLIENT_ID).append("&refresh_token=").append(refreshToken)
						.append("&grant_type=refresh_token&resource=https%3A%2F%2Fgraph.windows.net").toString();
		String path = new StringBuilder("https://login.microsoftonline.com/").append(TENANT_ID).append("/oauth2/token").toString();
		byte[] data = params.getBytes(encoding);
		URL url = new URL(path);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setRequestProperty("Content-Length", String.valueOf(data.length));
		conn.setConnectTimeout(5 * 1_000);

		try (OutputStream outputStream = conn.getOutputStream()) {
			outputStream.write(data);
			outputStream.flush();
		}

		return (conn.getResponseCode() != 200)
				? AzureADClientHelper.getLoginErrorFromString(HttpClientHelper.getResponseStringFromConn(conn, false))
				: AzureADClientHelper.getRefreshTokenOutputFromString(HttpClientHelper.getResponseStringFromConn(conn, true));
	}
}
