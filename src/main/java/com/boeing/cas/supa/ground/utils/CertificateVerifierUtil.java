package com.boeing.cas.supa.ground.utils;

import java.io.ByteArrayInputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(KeyVaultProperties.class)
public class CertificateVerifierUtil {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
    private KeyVaultProperties keyVaultProperties;


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
	
	public boolean IsValidClientCertificate(String certHeader, String certHolder){
		//Refer: https://docs.microsoft.com/en-us/azure/app-service/app-service-web-configure-tls-mutual-auth
		X509Certificate x509ClientCert = getCertFromHeader(certHeader);
		if(x509ClientCert != null){
			try {
				boolean isSelfSignedCert = isSelfSignedCert(x509ClientCert);
				logger.info("Certificate received is self signed:" + (isSelfSignedCert ? "true": "false"));
				if(!isSelfSignedCert){
					KeyVaultRetriever kvr = new KeyVaultRetriever(keyVaultProperties.getClientId(), keyVaultProperties.getClientKey());
					X509Certificate x509ServerCert = kvr.getCertificateByCertName(certHolder);
					Map<String, String> x509ClientCertSubjectDn = getMap(x509ClientCert.getSubjectDN().getName());
					Map<String, String> x509ClientCertIssuerDn = getMap(x509ClientCert.getIssuerDN().getName());
					String x509ClientCertThumbPrint = getThumbprint(x509ClientCert);
					Map<String, String> x509ServerCertSubjectDn = getMap(x509ServerCert.getSubjectDN().getName());
					Map<String, String> x509ServerCertIssuerDn = getMap(x509ServerCert.getIssuerDN().getName());
					String x509ServerCertThumbPrint = getThumbprint(x509ServerCert);
					
					
					//1. The certificate is not expired and is active for the current time on server.
					x509ClientCert.checkValidity(new Date());
					
					//2. Check subject name of certificate
					boolean foundSubject = false;
					if(x509ClientCertSubjectDn.get("CN").equals(x509ServerCertSubjectDn.get("CN"))){
						foundSubject = true;
					}
					
					if (!foundSubject) return false;
					
					//3. Check issuer name of certificate
					boolean foundIssuerCN = false, foundIssuerO = false;
					if(x509ClientCertIssuerDn.get("CN").equals(x509ServerCertIssuerDn.get("CN"))){
						foundIssuerCN = true;
					}
					if(x509ClientCertIssuerDn.get("O").equals(x509ServerCertIssuerDn.get("O"))){
						foundIssuerO = true;
					}
					if (!foundIssuerCN || !foundIssuerO) return false;
					
					//4. check the thumbprint of certificate
					if(!x509ServerCertThumbPrint.equalsIgnoreCase(x509ClientCertThumbPrint)){
						return false;
					}
					return true;
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
	 * @param cert 
	 */
	private boolean isSelfSignedCert(X509Certificate cert)
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

	private String getThumbprint(X509Certificate cert)
			throws NoSuchAlgorithmException, CertificateEncodingException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] der = cert.getEncoded();
		md.update(der);
		byte[] digest = md.digest();
		String digestHex = DatatypeConverter.printHexBinary(digest);
		return digestHex.toLowerCase();
	}

	
	private Map<String, String> getMap(String str){
		Map<String, String> tokens = Arrays.stream(str.split(","))
				.map(item -> item.split("="))
				.collect(Collectors.toMap(item -> item[0].trim(), item -> item[1].trim()));
		return tokens;
	}


}
