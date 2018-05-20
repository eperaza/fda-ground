package com.boeing.cas.supa.ground;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boeing.cas.supa.ground.pojos.KeyVaultProperties;
import com.boeing.cas.supa.ground.utils.KeyVaultRetriever;

@RestController
@EnableAutoConfiguration
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

		appSecrets.put("AzureADTenantName", keyVaultRetriever.getSecretByKey("AzureADTenantName"));
		appSecrets.put("AzureADTenantID", keyVaultRetriever.getSecretByKey("AzureADTenantID"));
		appSecrets.put("AzureADTenantAuthEndpoint", keyVaultRetriever.getSecretByKey("AzureADTenantAuthEndpoint"));
		appSecrets.put("AzureADAppClientID", keyVaultRetriever.getSecretByKey("AzureADAppClientID"));
		appSecrets.put("client1base64", keyVaultRetriever.getSecretByKey("client1base64"));
		appSecrets.put("client2base64", keyVaultRetriever.getSecretByKey("client2base64"));
		appSecrets.put("MailServerAuthPassword", keyVaultRetriever.getSecretByKey("MailServerAuthPassword"));
		appSecrets.put("MailServerAuthUsername", keyVaultRetriever.getSecretByKey("MailServerAuthUsername"));
		appSecrets.put("MailServerHost", keyVaultRetriever.getSecretByKey("MailServerHost"));
		appSecrets.put("MailServerPort", keyVaultRetriever.getSecretByKey("MailServerPort"));
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

	@Bean
	public Map<String, X509Certificate> getAppCertificates(KeyVaultRetriever keyVaultRetriever) {

		Map<String, X509Certificate> appCertificates = new ConcurrentHashMap<String, X509Certificate>();

		appCertificates.put("client1", keyVaultRetriever.getCertificateByCertName("client1"));
		appCertificates.put("client2", keyVaultRetriever.getCertificateByCertName("client2"));

		return appCertificates;
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

	@Bean
	public DataSource dataSource(KeyVaultRetriever keyVaultRetriever) {

		DataSourceBuilder factory = DataSourceBuilder.create()
				.driverClassName(keyVaultRetriever.getSecretByKey("SQLDriverClassName"))
				.type(BasicDataSource.class)
				.url(keyVaultRetriever.getSecretByKey("SQLDatabaseUrl"))
				.username(keyVaultRetriever.getSecretByKey("SQLDatabaseUsername"))
				.password(keyVaultRetriever.getSecretByKey("SQLDatabasePassword"));

		return factory.build();
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
