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
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private static String keyvaultUri = "https://fda-ground-kv.vault.azure.net/";
	
	private static KeyVaultClient kvc;

	
	public KeyVaultRetriever(String clientId, String clientKey) {
		kvc = KeyVaultAuthenticator.getAuthenticatedClient(clientId, clientKey);
	}

	public String getSecretByKey(String secretName){
		SecretBundle sb = kvc.getSecret(keyvaultUri, secretName);
		String secretValue = null;
		if(sb != null){
			secretValue = sb.value();
		}
		return secretValue;
	}
	
	public X509Certificate getCertificateByCertName(String certName){
		CertificateBundle cb = kvc.getCertificate(keyvaultUri, certName);
		X509Certificate x509Certificate = null;
		if(cb != null){
			try {
				x509Certificate = loadCerToX509Certificate(cb);
			} catch (CertificateException e) {
				logger.error(e.getMessage());
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}
		return x509Certificate;
		
	}

	private X509Certificate loadCerToX509Certificate(CertificateBundle certificateBundle) throws CertificateException, IOException {
        ByteArrayInputStream cerStream = new ByteArrayInputStream(certificateBundle.cer());
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate x509Certificate = (X509Certificate) certificateFactory.generateCertificate(cerStream);
        cerStream.close();
        return x509Certificate;
	}
}
