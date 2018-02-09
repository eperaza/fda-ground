package com.boeing.cas.supa.ground.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.boeing.cas.supa.ground.pojos.Error;

import org.springframework.core.io.InputStreamResource;

import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Controller
public class TSPFileController {
	@RequestMapping(value = "/tspFile", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<Object> downloadJSONFile(@RequestParam("tail") String tail){
		Error error;
		if(tail != null){
			HttpHeaders headers = new HttpHeaders();
		    headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		    headers.add("Pragma", "no-cache");
		    headers.add("Expires", "0");
		    String fileparam=tail+".json";
		    ClassPathResource tspFile = new ClassPathResource(fileparam);
		    if(tspFile.exists()){
		    	try {
					return ResponseEntity
					        .ok()
					        .headers(headers)
					        .contentLength(tspFile.contentLength())
					        .contentType(MediaType.parseMediaType("application/octet-stream"))
					        .body(new InputStreamResource(tspFile.getInputStream()));
				} catch (Exception e) {
					error = new Error("internal-error", e.getMessage());
				}
		    }
		}
		error = new Error("file_not_found", "Resource you are looking for doesn't exist");
		return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
		
	}
}
