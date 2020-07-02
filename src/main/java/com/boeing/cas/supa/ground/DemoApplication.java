package com.boeing.cas.supa.ground;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@EnableAutoConfiguration(exclude = {ErrorMvcAutoConfiguration.class})
@SpringBootApplication
@EnableConfigurationProperties(KeyVaultProperties.class)
public class DemoApplication {

	private final Logger logger = LoggerFactory.getLogger(DemoApplication.class);

	@Autowired
	private KeyVaultProperties keyVaultProperties;

	@Bean
	public KeyVaultRetriever getKeyVaultRetriever() {

		return new KeyVaultRetriever(this.keyVaultProperties.getUri(), this.keyVaultProperties.getClientId(),
				this.keyVaultProperties.getClientKey());
	}

	@Bean
	public Map<String,String> getAppSecrets(KeyVaultRetriever keyVaultRetriever) {
		Map<String, String> appSecrets = new ConcurrentHashMap<String, String>();

		appSecrets.put("adwHost", keyVaultRetriever.getSecretByKey("adwHost"));
		appSecrets.put("adwPath", keyVaultRetriever.getSecretByKey("adwPath"));
		appSecrets.put("adwPwd", keyVaultRetriever.getSecretByKey("adwPwd"));
		appSecrets.put("adwUpload", keyVaultRetriever.getSecretByKey("adwUpload"));
		appSecrets.put("adwUploadFDA", keyVaultRetriever.getSecretByKey("adwUploadFDA"));
		appSecrets.put("adwUser", keyVaultRetriever.getSecretByKey("adwUser"));
		appSecrets.put("AzureADAppClientID", keyVaultRetriever.getSecretByKey("AzureADAppClientID"));
		appSecrets.put("AzureADCustomTenantName", keyVaultRetriever.getSecretByKey("AzureADCustomTenantName"));
		appSecrets.put("AzureADTenantAuthEndpoint", keyVaultRetriever.getSecretByKey("AzureADTenantAuthEndpoint"));
		appSecrets.put("AzureADTenantID", keyVaultRetriever.getSecretByKey("AzureADTenantID"));
		appSecrets.put("AzureADTenantName", keyVaultRetriever.getSecretByKey("AzureADTenantName"));
		appSecrets.put("EmailMpAttachmentLocation", keyVaultRetriever.getSecretByKey("EmailMpAttachmentLocation"));
		appSecrets.put("FDAdvisorClientCertName", keyVaultRetriever.getSecretByKey("FDAdvisorClientCertName"));
		//not ready for this yet
//		appSecrets.put("FDAdvisorRegistrationCertName", keyVaultRetriever.getSecretByKey("FDAdvisorRegistrationCertName"));
		appSecrets.put("FDAdvisorClientCertBundlePassword", keyVaultRetriever.getSecretByKey("FDAdvisorClientCertBundlePassword"));
		String fdaClientCertBase64 = new StringBuilder(appSecrets.get("FDAdvisorClientCertName")).append("base64").toString();
		appSecrets.put(fdaClientCertBase64, keyVaultRetriever.getSecretByKey(fdaClientCertBase64));
		appSecrets.put("MailServerAuthPassword", keyVaultRetriever.getSecretByKey("MailServerAuthPassword"));
		appSecrets.put("MailServerAuthUsername", keyVaultRetriever.getSecretByKey("MailServerAuthUsername"));
		appSecrets.put("MailServerHost", keyVaultRetriever.getSecretByKey("MailServerHost"));
		appSecrets.put("MailServerPort", keyVaultRetriever.getSecretByKey("MailServerPort"));
		appSecrets.put("RouteSyncClientCert", keyVaultRetriever.getSecretByKey("RouteSyncClientCert"));
		appSecrets.put("RouteSyncClientCertPassword", keyVaultRetriever.getSecretByKey("RouteSyncClientCertPassword"));
		appSecrets.put("RouteSyncServerCert", keyVaultRetriever.getSecretByKey("RouteSyncServerCert"));
		appSecrets.put("RouteSyncServerTrustStoreAlias", keyVaultRetriever.getSecretByKey("RouteSyncServerTrustStoreAlias"));
		appSecrets.put("RouteSyncFOMUrl", keyVaultRetriever.getSecretByKey("RouteSyncFOMUrl"));
		appSecrets.put("SQLDatabasePassword", keyVaultRetriever.getSecretByKey("SQLDatabasePassword"));
		appSecrets.put("SQLDatabaseUrl", keyVaultRetriever.getSecretByKey("SQLDatabaseUrl"));
		appSecrets.put("SQLDatabaseUsername", keyVaultRetriever.getSecretByKey("SQLDatabaseUsername"));
		appSecrets.put("SQLDriverClassName", keyVaultRetriever.getSecretByKey("SQLDriverClassName"));
		appSecrets.put("StorageAccountName", keyVaultRetriever.getSecretByKey("StorageAccountName"));
		appSecrets.put("StorageKey", keyVaultRetriever.getSecretByKey("StorageKey"));
		appSecrets.put("UserManagementAppClientId", keyVaultRetriever.getSecretByKey("UserManagementAppClientId"));
		appSecrets.put("UserManagementAppClientSecret", keyVaultRetriever.getSecretByKey("UserManagementAppClientSecret"));
		appSecrets.put("UserManagementAdminPassword", keyVaultRetriever.getSecretByKey("UserManagementAdminPassword"));
		appSecrets.put("UserManagementAdminUsername", keyVaultRetriever.getSecretByKey("UserManagementAdminUsername"));

		return appSecrets;
	}

	@Bean(name = "appCertificates")
	public Map<String, X509Certificate> getAppCertificates(KeyVaultRetriever keyVaultRetriever) {

		Map<String, X509Certificate> appCertificates = new ConcurrentHashMap<String, X509Certificate>();
		String fdaClientCertName = keyVaultRetriever.getSecretByKey("FDAdvisorClientCertName");
		appCertificates.put(fdaClientCertName, keyVaultRetriever.getCertificateByCertName(fdaClientCertName));
		return appCertificates;
	}

	@Bean(name = "appRegistrationCertificates")
	public Map<String, X509Certificate> getAppRegistrationCertificates(KeyVaultRetriever keyVaultRetriever) {

		Map<String, X509Certificate> appRegistrationCertificates = new ConcurrentHashMap<String, X509Certificate>();
		//need to remove hard-coded names, and use keyvault entries instead
		String fdaRegistrationCertName = "fdadvisor2z";
		appRegistrationCertificates.put(fdaRegistrationCertName, keyVaultRetriever.getCertificateByCertName(fdaRegistrationCertName));
		return appRegistrationCertificates;
	}

	@Bean
    public SSLSocketFactory getSSLSocketFactory(Map<String,String> appSecrets) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, KeyManagementException {

    	SSLSocketFactory sslSocketFactory = null;
    	KeyStore ks = KeyStore.getInstance("PKCS12");
    	File clientCertFile = new ClassPathResource(appSecrets.get("RouteSyncClientCert")).getFile();
    	File serverCertFile = new ClassPathResource(appSecrets.get("RouteSyncServerCert")).getFile();
    	logger.debug("loaded client cert PFX file");
    	try (FileInputStream fis = new FileInputStream(clientCertFile)) {

    		ks.load(fis, appSecrets.get("RouteSyncClientCertPassword").toCharArray());
    		logger.debug("loaded PFX into keystore");
    		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    		kmf.init(ks, appSecrets.get("RouteSyncClientCertPassword").toCharArray());

    		try (InputStream is = new FileInputStream(serverCertFile)) {

        		CertificateFactory cf = CertificateFactory.getInstance("X.509");
        		X509Certificate caCert = (X509Certificate) cf.generateCertificate(is);
        		logger.info("subject = {}, issuer = {}", caCert.getSubjectDN(), caCert.getIssuerDN());
        		ks.setCertificateEntry(appSecrets.get("RouteSyncServerTrustStoreAlias"), caCert);
    		}

    		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
    		SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
    		logger.debug("initialized SSLContext");
    		sslSocketFactory = sc.getSocketFactory();
    	} catch (Exception ex) {
    		logger.error("Error creatomg SSLSocketFactory: {}", ex.getMessage(), ex);
    	}
    	
    	return sslSocketFactory;
	}

	@Bean
	public JavaMailSender javaMailSender(KeyVaultRetriever keyVaultRetriever) {

		JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();

		final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
		final String MAIL_TRANSPORT_PROTOCOL = "smtp";
		final int MAIL_SERVER_PORT = Integer.parseInt(keyVaultRetriever.getSecretByKey("MailServerPort"), 10);
		Properties mailProperties = new Properties();
		mailProperties.put("mail.transport.protocol", MAIL_TRANSPORT_PROTOCOL);
		mailProperties.put("mail.smtp.auth", true);
		mailProperties.put("mail.smtp.starttls.enable", true);
		mailProperties.put("mail.smtp.socketFactory.port", MAIL_SERVER_PORT);
		mailProperties.put("mail.debug", true);
		mailProperties.put("mail.smtp.socketFactory.class", SSL_FACTORY);
		mailProperties.put("mail.smtp.connectiontimeout", 20_000);
		mailProperties.put("mail.smtp.timeout", 10_000);
		mailProperties.put("mail.smtp.writetimeout", 20_000);

		javaMailSender.setJavaMailProperties(mailProperties);
		javaMailSender.setHost(keyVaultRetriever.getSecretByKey("MailServerHost"));
		javaMailSender.setPort(MAIL_SERVER_PORT);
		javaMailSender.setProtocol(MAIL_TRANSPORT_PROTOCOL);
		javaMailSender.setUsername(keyVaultRetriever.getSecretByKey("MailServerAuthUsername"));
		javaMailSender.setPassword(keyVaultRetriever.getSecretByKey("MailServerAuthPassword"));
		javaMailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());

		return javaMailSender;
	}

	@RequestMapping("/")
	public ResponseEntity<Map<String, String>> getGreeting(HttpServletRequest httpRequest) {

		logger.info("Pinging the service");
		Map<String, String> responseMap = new HashMap<>();
		responseMap.put("ping:", "hello world");
		return new ResponseEntity<>(responseMap, HttpStatus.OK);
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}
