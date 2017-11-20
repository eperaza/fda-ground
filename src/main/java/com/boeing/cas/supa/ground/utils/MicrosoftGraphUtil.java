package com.boeing.cas.supa.ground.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boeing.cas.supa.ground.helpers.HttpClientHelper;
import com.boeing.cas.supa.ground.pojos.User;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;


public class MicrosoftGraphUtil {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private String tenant;
	private String accessToken;
	public MicrosoftGraphUtil(String tenant, String accessToken) {
		this.tenant = tenant;
		this.accessToken = accessToken;
	}
	public User getUsernamesFromGraph(String uniqueId) {
        logger.debug("get user object info from graph");
        User userClass = null;
		URL url;
		try {
			url = new URL(String.format("https://graph.windows.net/%s/users/%s?api-version=2013-04-05", tenant,uniqueId));
			logger.debug("url: " + url.toString());
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        // Set the appropriate header fields in the request header.
	        conn.setRequestProperty("api-version", "2013-04-05");
	        conn.setRequestProperty("Authorization", accessToken);
	        conn.setRequestProperty("Accept", "application/json;odata=minimalmetadata");
	        String goodRespStr = HttpClientHelper.getResponseStringFromConn(conn, true);
	        logger.info("goodRespStr ->" + goodRespStr);
	        int responseCode = conn.getResponseCode();
	        logger.info("responseCode: " + responseCode);
	        ObjectMapper objectMapper = new ObjectMapper();
	        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	        ObjectReader objectReader = objectMapper.reader(User.class);
	        userClass = objectReader.readValue(goodRespStr);
	        return userClass;
		} catch (MalformedURLException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		return userClass;
        
    }
}