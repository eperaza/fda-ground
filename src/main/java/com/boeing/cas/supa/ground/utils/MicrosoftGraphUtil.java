package com.boeing.cas.supa.ground.utils;

import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boeing.cas.supa.ground.helpers.HttpClientHelper;
import com.boeing.cas.supa.ground.helpers.JSONHelper;
import com.boeing.cas.supa.ground.pojos.User;


public class MicrosoftGraphUtil {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	 public User getUsernamesFromGraph(String accessToken, String tenant, String userId) throws Exception {
	    	URL url = new URL(String.format("https://graph.windows.net/%s/users/%s",
                    tenant, userId, accessToken));

	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        // Set the appropriate header fields in the request header.
	        conn.setRequestProperty("api-version", "2013-04-05");
	        conn.setRequestProperty("Authorization", accessToken);
	        conn.setRequestProperty("Accept", "application/json;odata=minimalmetadata");
	        String goodRespStr = HttpClientHelper.getResponseStringFromConn(conn, true);
	        logger.info("goodRespStr ->" + goodRespStr);
	        int responseCode = conn.getResponseCode();
	        logger.info("responseCode => " + responseCode);
	        return null;
	    }
}
