package com.boeing.cas.supa.ground.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boeing.cas.supa.ground.helpers.HttpClientHelper;
import com.boeing.cas.supa.ground.pojos.Group;
import com.boeing.cas.supa.ground.pojos.User;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class MicrosoftGraphUtil {

    private final Logger logger = LoggerFactory.getLogger(MicrosoftGraphUtil.class);

    private static final String AZURE_AD_GRAPH_API_VERSION = "api-version=1.6";
    
    private String tenant;
	private String accessToken;

	public MicrosoftGraphUtil(String tenant, String accessToken) {
		this.tenant = tenant;
		this.accessToken = accessToken;
	}

	public User getUsernamesFromGraph(String uniqueId) {

		logger.debug("Getting user object info from graph");
        User userObj = null;
		try {

			URL url = new URL(
					new StringBuilder("https://graph.windows.net/").append(this.tenant)
						.append("/users/").append(uniqueId)
						.append('?').append(AZURE_AD_GRAPH_API_VERSION)
						.toString());
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        // Set the appropriate header fields in the request header.
	        conn.setRequestProperty("api-version", AZURE_AD_GRAPH_API_VERSION.replace(Constants.AZURE_API_VERSION_PREFIX, StringUtils.EMPTY));
	        conn.setRequestProperty("Authorization", accessToken);
	        conn.setRequestProperty("Accept", Constants.ACCEPT_CT_JSON_ODATAMINIMAL);

	        String responseStr = HttpClientHelper.getResponseStringFromConn(conn, true);
	        int responseCode = conn.getResponseCode();

	        ObjectMapper objectMapper = new ObjectMapper();
	        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			ObjectReader objectReader = objectMapper.readerFor(User.class);
	        if (responseCode == 200) {
	        	userObj = objectReader.readValue(responseStr);
	        }
		} catch (MalformedURLException e) {
            this.logger.error("MalformedURLException while retrieving user names from graph: {}", e.getMessage(), e);
        }
        catch (IOException e) {
            this.logger.error("I/O Exception while retrieving user names from graph: {}", e.getMessage(), e);
        }

		return userObj;
    }

	public List<Group> getGroupFromGraph(String uniqueId) {

		logger.debug("Getting group object list info from graph");
		List<Group> groupList = new ArrayList<>();
		try {

			URL url = new URL(
					new StringBuilder("https://graph.windows.net/").append(this.tenant)
						.append("/users/").append(uniqueId)
						.append("/memberOf")
						.append('?').append(AZURE_AD_GRAPH_API_VERSION)
						.toString());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			// Set the appropriate header fields in the request header.
	        conn.setRequestProperty("api-version", AZURE_AD_GRAPH_API_VERSION.replace(Constants.AZURE_API_VERSION_PREFIX, StringUtils.EMPTY));
	        conn.setRequestProperty("Authorization", accessToken);
	        conn.setRequestProperty("Accept", Constants.ACCEPT_CT_JSON_ODATAMINIMAL);
	        String responseStr = HttpClientHelper.getResponseStringFromConn(conn, true);
	        groupList = getGroupListFromString(responseStr);
	        int responseCode = conn.getResponseCode();
	        if (responseCode == 200){
	        	groupList = getGroupListFromString(responseStr);
	        }
		} catch (MalformedURLException e) {
			logger.error("MalformedURLException while retrieving groups from graph: {}", e.getMessage(), e);
		} catch (IOException e) {
			logger.error("I/O Exception while retrieving groups from graph: {}", e.getMessage(), e);
		}

		return groupList;        
    }

	public List<Group> getGroupListFromString(String responseStr) {

		List<Group> groupList = new ArrayList<>();
		try {

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			JsonNode arrNode = mapper.readTree(responseStr).get("value");			
			if (arrNode.isArray()) {

				for (JsonNode objNode : arrNode) {
					Group group = mapper.treeToValue(objNode, Group.class);
					logger.info(group.toString());
					groupList.add(group);
			    }
			}
		} catch (IOException e) {
			logger.error("I/O Exception while reading groups from list: {}", e.getMessage(), e);
		}

		return groupList;
	}
}
