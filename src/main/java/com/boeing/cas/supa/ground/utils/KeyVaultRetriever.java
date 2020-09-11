package com.boeing.cas.supa.ground.utils;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.CertificateBundle;
import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.models.SecretBundle;
import com.microsoft.azure.keyvault.webkey.JsonWebKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class KeyVaultRetriever {

	private final Logger logger = LoggerFactory.getLogger(KeyVaultRetriever.class);

	private String keyVaultUri;

	private KeyVaultClient kvc;

	public KeyVaultRetriever(String keyVaultUri, String clientId, String clientKey) {

		this.keyVaultUri = keyVaultUri;
		this.kvc = KeyVaultAuthenticator.getAuthenticatedClient(clientId, clientKey);

		System.setProperty("http.proxyHost", "www-only-ewa-proxy.web.boeing.com");
		System.setProperty("http.proxyPort", "31061");
		System.setProperty("https.proxyHost", "www-only-ewa-proxy.web.boeing.com");
		System.setProperty("https.proxyPort", "31061");
	}

	public String getSecretByKey(String secretName) {

		SecretBundle sb = this.kvc.getSecret(this.keyVaultUri, secretName);
		String secretValue = null;
		if (sb != null) {
			secretValue = sb.value();
		}

		return secretValue;
	}

	public JsonWebKey getPrivateKeyByKeyName(String keyName) {

		KeyBundle kb = this.kvc.getKey(this.keyVaultUri, keyName);
		JsonWebKey key = null;
		if (kb != null) {

			key = kb.key();
			logger.info("JsonWebKey entry has private key: {}", key.hasPrivateKey());
			KeyPair kp = key.toRSA(key.hasPrivateKey());
			logger.info("private key: {}", kp.getPrivate());
			logger.info("public key: {}", kp.getPublic());
		}

		return key;
	}

    public X509Certificate getCertificateByCertName(String certName) {

		CertificateBundle cb = this.kvc.getCertificate(this.keyVaultUri, certName);
		X509Certificate x509Certificate = null;

		if (cb != null) {

			try {
				x509Certificate = this.loadCerToX509Certificate(cb);
			} catch (CertificateException ce) {
                logger.error("CertificateException: {}", ce.getMessage(), ce);
            }
            catch (IOException ioe) {
                logger.error("IOException: {}", ioe.getMessage(), ioe);
            }
		}

		return x509Certificate;
	}

	private X509Certificate loadCerToX509Certificate(CertificateBundle certificateBundle) throws CertificateException, IOException {

		X509Certificate x509Certificate = null;

		try (ByteArrayInputStream cerStream = new ByteArrayInputStream(certificateBundle.cer())) {
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			x509Certificate = (X509Certificate) certificateFactory.generateCertificate(cerStream);
		}

        return x509Certificate;
	}
}
