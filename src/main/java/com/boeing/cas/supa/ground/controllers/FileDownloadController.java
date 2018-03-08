package com.boeing.cas.supa.ground.controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

	@Autowired
    private KeyVaultProperties keyVaultProperties;
	
	@ResponseBody
	@RequestMapping(value = "/download", method = RequestMethod.GET)
	public byte[] downloadJSONFile(@RequestParam("tail") String tail, @RequestParam("type") String type, HttpServletRequest request, HttpServletResponse response){
		KeyVaultRetriever kvr = new KeyVaultRetriever(keyVaultProperties.getClientId(), keyVaultProperties.getClientKey());		
		
		try {
			AzureStorageUtil asu = new AzureStorageUtil(kvr.getSecretByKey("StorageKey"));
			if(tail != null){
				ByteArrayOutputStream outputStream = null;
				//Files are always case sensitive so need to change fileparam...PLEASE 
				if(type.toLowerCase().equals("tsp")){
					String fileparam=tail+".json";
					outputStream = asu.downloadFile("tsp", fileparam);
				}
				if(type.toLowerCase().equals("properties")){
					
					String fileparam = tail+".properties";
					outputStream = asu.downloadFile("properties", fileparam);
				}
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
