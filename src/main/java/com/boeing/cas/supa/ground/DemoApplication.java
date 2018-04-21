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

@RestController
@EnableAutoConfiguration
@SpringBootApplication
public class DemoApplication {
	
	private final Logger logger = LoggerFactory.getLogger(DemoApplication.class);

	@RequestMapping("/")
	public ResponseEntity<Map<String, String>> getGreeting(HttpServletRequest httpRequest)  {

		logger.info("Pinging the service");
		Map<String, String> responseMap = new HashMap<>();
		responseMap.put("ping:", "hello world");
		return new ResponseEntity<>(responseMap, HttpStatus.OK);
	}
	
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}
