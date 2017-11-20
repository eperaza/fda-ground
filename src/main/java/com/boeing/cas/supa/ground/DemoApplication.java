package com.boeing.cas.supa.ground;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;

@RestController
@EnableAutoConfiguration
@SpringBootApplication

public class DemoApplication {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	
	@RequestMapping("/")
	String home(HttpServletRequest httpRequest) {
		TelemetryClient telemetryClient = new TelemetryClient();
		telemetryClient.trackEvent("MobileBackendAPI main() in ping");
		TelemetryConfiguration.getActive().getChannel().setDeveloperMode(true);
		logger.info("Pinging the service");
		return "Ping!";
	}
	
	
	public static void main(String[] args) {
		System.setProperty("http.proxyHost", "www-only-ewa-proxy.web.boeing.com");
        System.setProperty("http.proxyPort", "31061");
        System.setProperty("https.proxyHost", "www-only-ewa-proxy.web.boeing.com");
        System.setProperty("https.proxyPort", "31061");
		TelemetryClient telemetryClient = new TelemetryClient();
		telemetryClient.trackEvent("MobileBackendAPI main() started");
		SpringApplication.run(DemoApplication.class, args);
	}
}
