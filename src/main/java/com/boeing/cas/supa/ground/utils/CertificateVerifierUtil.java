package com.boeing.cas.supa.ground.utils;

import java.io.ByteArrayInputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@EnableConfigurationProperties(KeyVaultProperties.class)
public class CertificateVerifierUtil {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
    private KeyVaultProperties keyVaultProperties;
	
	private X509Certificate cert;
	private String certHolder;
	

	
	public CertificateVerifierUtil(String clientCertHeader, String certHolder){
		this.cert = getCertFromHeader(clientCertHeader);
		this.certHolder = certHolder;
	}
	private X509Certificate getCertFromHeader(String certHeader) {
		X509Certificate cert = null;
		if(!certHeader.isEmpty() || certHeader != null){
			byte[] decodedBytes = Base64.getDecoder().decode(certHeader);
			CertificateFactory cf;
			try {
				cf = CertificateFactory.getInstance("X.509");
				cert = (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(decodedBytes));
			} catch (Exception e ) {
				cert = null;
				logger.info(e.getMessage());
			}
		}
		return cert;
	}
	
	public boolean IsValidClientCertificate(){
		if(cert != null){
			try {
				boolean isSelfSignedCert = isSelfSignedCert();
				logger.info("Certificate received is self signed:" + (isSelfSignedCert ? "true": "false"));
				if(!isSelfSignedCert){
					KeyVaultRetriever kvr = new KeyVaultRetriever(keyVaultProperties.getClientId(), keyVaultProperties.getClientKey());
					X509Certificate x509 = kvr.getCertificateByCertName(certHolder);
					return x509.equals(cert);
				}
			} catch (CertificateException | NoSuchAlgorithmException | NoSuchProviderException e) {
				logger.debug("Some exceptions are happening in certificate verification");
				logger.debug(e.getMessage());
				return false;
			}

		}
		return false;

	}
	/**
	 * Checks whether given X.509 certificate is self-signed.
	 */
	private boolean isSelfSignedCert()
			throws CertificateException, NoSuchAlgorithmException,
			NoSuchProviderException {
		try {
			// Try to verify certificate signature with its own public key
			PublicKey key = cert.getPublicKey();
			cert.verify(key);
			return true;
		} catch (SignatureException sigEx) {
			// Invalid signature --> not self-signed
			return false;
		} catch (InvalidKeyException keyEx) {
			// Invalid key --> not self-signed
			return false;
		}
	}



}
