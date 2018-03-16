package com.boeing.cas.supa.ground.controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.boeing.cas.supa.ground.utils.KeyVaultProperties;
import com.boeing.cas.supa.ground.utils.KeyVaultRetriever;

@Controller
@EnableConfigurationProperties(KeyVaultProperties.class)
public class FileDownloadController {
	
//	private static final Map<String, String> CONTAINER_FILE_TYPE_MAP = createMap();
//    private static Map<String, String> createMap()
//    {
//        Map<String,String> myMap = new HashMap<String,String>();
//        myMap.put("tsp", "json");
//        myMap.put("config", "mobileconfig");
//        myMap.put("properties", "properties");
//        return myMap;
//    }
    
	@Autowired
    private KeyVaultProperties keyVaultProperties;
	
	@ResponseBody
	@RequestMapping(value = "/download", method = RequestMethod.GET)
	public byte[] downloadJSONFile(@RequestParam("file") String file, @RequestParam("type") String type, HttpServletRequest request, HttpServletResponse response){
		KeyVaultRetriever kvr = new KeyVaultRetriever(keyVaultProperties.getClientId(), keyVaultProperties.getClientKey());		
		
		try {
			AzureStorageUtil asu = new AzureStorageUtil(kvr.getSecretByKey("StorageKey"));
			if(file != null){
				ByteArrayOutputStream outputStream = null;
				//Files are always case sensitive so no need to change fileparam...PLEASE 
				type = type.toLowerCase();
				String fileParam = file;
				outputStream = asu.downloadFile(type, fileParam);
				if(outputStream != null){
					response.setStatus( HttpServletResponse.SC_OK  );
					return outputStream.toByteArray(); 
				}else{
					response.setStatus( HttpServletResponse.SC_NOT_FOUND  );
					return null;
				}
			}			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		response.setStatus( HttpServletResponse.SC_BAD_REQUEST  );
		return null;
		
	}

	
}
