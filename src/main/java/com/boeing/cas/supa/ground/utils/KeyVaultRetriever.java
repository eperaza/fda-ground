package com.boeing.cas.supa.ground.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.CertificateBundle;
import com.microsoft.azure.keyvault.models.SecretBundle;

public class KeyVaultRetriever {

	private final Logger logger = LoggerFactory.getLogger(KeyVaultRetriever.class);

	private static String keyvaultUri = "https://fda-ground-kv.vault.azure.net/";

	private KeyVaultClient kvc;

	public KeyVaultRetriever(String clientId, String clientKey) {
		this.kvc = KeyVaultAuthenticator.getAuthenticatedClient(clientId, clientKey);
	}

	public String getSecretByKey(String secretName) {

		SecretBundle sb = this.kvc.getSecret(keyvaultUri, secretName);
		String secretValue = null;
		if (sb != null) {
			secretValue = sb.value();
		}

		return secretValue;
	}
	
	public X509Certificate getCertificateByCertName(String certName) {

		CertificateBundle cb = this.kvc.getCertificate(keyvaultUri, certName);
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
