package com.boeing.cas.supa.ground;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boeing.cas.supa.ground.helpers.HttpClientHelper;
import com.boeing.cas.supa.ground.pojos.User;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;

@RestController
@EnableAutoConfiguration
@SpringBootApplication


public class DemoApplication {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	
	@RequestMapping("/")
	public ResponseEntity<Map<String, Object>> getGreeting(HttpServletRequest httpRequest)  {
		TelemetryClient telemetryClient = new TelemetryClient();
		telemetryClient.trackEvent("MobileBackendAPI main() in ping");
		TelemetryConfiguration.getActive().getChannel().setDeveloperMode(true);
		logger.info("Pinging the service");
		Map<String, Object> responseMap = new HashMap<>();
		User user = HttpClientHelper.getUserInfoFromHeader(httpRequest);
		System.out.println(user.getGivenName());
		responseMap.put("greeting", "hello world");
		return new ResponseEntity<>(responseMap, HttpStatus.OK);
	}
	
	
	


	public static void main(String[] args) {
		TelemetryClient telemetryClient = new TelemetryClient();
		telemetryClient.trackEvent("MobileBackendAPI main() started");
		SpringApplication.run(DemoApplication.class, args);
	}
	
	
}
