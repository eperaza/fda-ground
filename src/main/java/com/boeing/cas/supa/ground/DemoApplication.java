package com.boeing.cas.supa.ground;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.microsoft.applicationinsights.TelemetryClient;

@RestController
@EnableAutoConfiguration
@SpringBootApplication


public class DemoApplication {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	
	@RequestMapping("/")
	public ResponseEntity<Map<String, Object>> getGreeting(HttpServletRequest httpRequest)  {
		logger.info("Pinging the service");
		Map<String, Object> responseMap = new HashMap<>();
		responseMap.put("ping:", "hello world");
		return new ResponseEntity<>(responseMap, HttpStatus.OK);
	}
	
	
	@RequestMapping(value = "/post", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
			method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
		public ResponseEntity<Map<String, Object>> echoBack(@RequestBody(required = false) byte[] rawBody, HttpServletRequest request) throws IOException {

			Map<String, String> headers = new HashMap<String, String>();
			for (String headerName : Collections.list(request.getHeaderNames())) {
				headers.put(headerName, request.getHeader(headerName));
			}

			Map<String, Object> responseMap = new HashMap<String,Object>();
			responseMap.put("protocol", request.getProtocol());
			responseMap.put("method", request.getMethod());
			responseMap.put("headers", headers);
			responseMap.put("cookies", request.getCookies());
			responseMap.put("parameters", request.getParameterMap());
			responseMap.put("path", request.getServletPath());
			responseMap.put("body", rawBody != null ? Base64.getEncoder().encodeToString(rawBody) : null);

			return ResponseEntity.status(HttpStatus.OK).body(responseMap);
		}
	
	


	public static void main(String[] args) {
		TelemetryClient telemetryClient = new TelemetryClient();
		telemetryClient.trackEvent("MobileBackendAPI main() started");
		SpringApplication.run(DemoApplication.class, args);
	}
	
	
}
