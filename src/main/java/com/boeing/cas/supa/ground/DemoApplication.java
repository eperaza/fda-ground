package com.boeing.cas.supa.ground;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boeing.cas.supa.ground.utils.KeyVaultProperties;
import com.boeing.cas.supa.ground.utils.KeyVaultRetriever;
import com.microsoft.applicationinsights.TelemetryClient;

@RestController
@EnableAutoConfiguration
@SpringBootApplication

@EnableConfigurationProperties(KeyVaultProperties.class)
public class DemoApplication {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
    private KeyVaultProperties keyVaultProperties;
	
	@RequestMapping("/")
	public ResponseEntity<Map<String, Object>> getGreeting(HttpServletRequest httpRequest)  {
		try {
			String getPfxEncodedAsBase64KeyVault = getPfxEncodedAsBase64KeyVault();
			System.out.println(getPfxEncodedAsBase64KeyVault);
		} catch (IOException | CertificateEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		logger.info("Pinging the service");
		Map<String, Object> responseMap = new HashMap<>();
		responseMap.put("ping:", "hello world");
		return new ResponseEntity<>(responseMap, HttpStatus.OK);
	}
	
	public static void main(String[] args) {
		
		TelemetryClient telemetryClient = new TelemetryClient();
		telemetryClient.trackEvent("MobileBackendAPI main() started");
		SpringApplication.run(DemoApplication.class, args);
	}
	
	private String getPfxEncodedAsBase64() throws IOException{
		InputStream fis = this.getClass().getClassLoader().getResourceAsStream("client2.pfx");;
		ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
		byte[] InBuffer = new byte[512];
		int read = 0;
		while ( (read = fis.read(InBuffer)) != -1 ) {
		   outBuffer.write(InBuffer, 0, read);
		}
		outBuffer.toByteArray().toString();
		String encoded = Base64.getEncoder().encodeToString(outBuffer.toByteArray());
		return encoded;

	}
	
	private String getPfxEncodedAsBase64KeyVault() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		KeyVaultRetriever kvr = new KeyVaultRetriever(keyVaultProperties.getClientId(), keyVaultProperties.getClientKey());
		X509Certificate x509 = kvr.getCertificateByCertName("client2");
		String key = kvr.getSecretByKey("StorageKey");
		return x509.toString();
	}
	
	
}
