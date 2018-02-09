package com.boeing.cas.supa.ground.helpers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.boeing.cas.supa.ground.pojos.Error;
import com.boeing.cas.supa.ground.pojos.RefreshTokenOutput;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AzureADClientHelper {

	public static Error getLoginErrorFromString(String responseString) {
		ObjectMapper mapper = new ObjectMapper(); 
		TypeReference<HashMap<String, String>> typeRef 
		  = new TypeReference<HashMap<String, String>>() {};
		try {
			Map<String, String> map = mapper.readValue(responseString, HashMap.class);
			String errorDesc = map.get("error_description");
			String errorString = errorDesc.split("\\r?\\n")[0];
			return new Error(map.get("error"), errorString);
		} catch (IOException e) {
			return null;
		}
	}

	public static RefreshTokenOutput getRefreshTokenOutputFromString(String responseString) {
		ObjectMapper mapper = new ObjectMapper(); 
		TypeReference<HashMap<String, String>> typeRef 
		  = new TypeReference<HashMap<String, String>>() {};
		try{
			Map<String, String> map = mapper.readValue(responseString, typeRef);
			String token_type = map.get("token_type");
			String expires_in = map.get("expires_in");
			String expires_on = map.get("expires_on");
			String not_before = map.get("not_before");
			String access_token = map.get("access_token");
			String refresh_token = map.get("refresh_token");
			return new RefreshTokenOutput(token_type, expires_in, expires_on, not_before, access_token, refresh_token);
		} catch (IOException e) {
			return null;
		}
	}

}
