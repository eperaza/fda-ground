package com.boeing.cas.supa.ground;

import java.text.ParseException;
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

import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.utils.HeaderMapRequestWrapper;
import com.boeing.cas.supa.ground.utils.MicrosoftGraphUtil;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.nimbusds.jwt.JWTParser;

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
		HeaderMapRequestWrapper header = new HeaderMapRequestWrapper(httpRequest);
		String tokenName = header.getHeader("authorization");
		String uniqueId = getUniqueIdFromJWT(tokenName);
		MicrosoftGraphUtil mgu = new MicrosoftGraphUtil("fdacustomertest.onmicrosoft.com", tokenName.replaceFirst("Bearer ", ""));
		User user = mgu.getUsernamesFromGraph(uniqueId);
		Map<String, Object> responseMap = new HashMap<>();
		responseMap.put("greeting", "hello world");
		return new ResponseEntity<>(responseMap, HttpStatus.OK);
	}
	
	
	private String getUniqueIdFromJWT(String xAuth) {
		String uniqueId = null;
		if(xAuth.contains("Bearer")){
    		xAuth = xAuth.replaceFirst("Bearer ", "");
    		Map<String, Object> claimsMap;
			try {
				claimsMap = JWTParser.parse(xAuth).getJWTClaimsSet().getClaims();
				uniqueId = (String) claimsMap.get("oid");
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
		}
		return uniqueId;
	}


	public static void main(String[] args) {
		TelemetryClient telemetryClient = new TelemetryClient();
		telemetryClient.trackEvent("MobileBackendAPI main() started");
		SpringApplication.run(DemoApplication.class, args);
	}
	
	
}
