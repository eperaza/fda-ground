package com.boeing.cas.supa.ground.utils;

import java.io.ByteArrayInputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CertificateVerifierUtil {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private X509Certificate cert;
	public CertificateVerifierUtil(String clientCertHeader){
		this.cert = getCertFromHeader(clientCertHeader);
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
					//1. The certificate is not expired and is active for the current time on server.
					cert.checkValidity(new Date());

					//2. The subject name of the certificate has the common name nildevecc
					//subjectDn: CN=a5605027.mw.nos.boeing.com
					//EMAILADDRESS=certs@example.com, CN=client1, OU=techops, O=Example Co, L=Boston, ST=MA, C=US
					Principal principal = cert.getSubjectDN();
					String subjectDn = principal.getName();
					logger.info("Certificate received with subjectDn: " +subjectDn);
					boolean foundSubject = false;
					List<String> subjectDnList = Arrays.asList(subjectDn.split(","));
					for(String subject: subjectDnList){
						if(subject.trim().equalsIgnoreCase("CN=client1")){
							foundSubject = true;
							break;
						}
						if(subject.trim().equalsIgnoreCase("CN=a5605027.mw.nos.boeing.com")){
							foundSubject = true;
							break;
						}
					}




					//3. Check issuer name of certificate
					//CN=Boeing Class 2 Windows Machines, OU=certservers, O=Boeing, C=US
					//EMAILADDRESS=certs@example.com, CN=ca, OU=techops, O=Example Co, L=Boston, ST=MA, C=US

					if (!foundSubject) return false;

					principal = cert.getIssuerDN();
					String issuerDn = principal.getName();
					List<String> issuerDnList = Arrays.asList(issuerDn.split(","));
					logger.info("Certificate received with issuerDn: " +issuerDn);
					boolean foundIssuerCN = false, foundIssuerO = false;
					for(String issuer: issuerDnList){
						if(issuer.trim().equalsIgnoreCase("CN=ca")){
							foundIssuerCN = true;
							if (foundIssuerO) break;
						}
						if(issuer.trim().equalsIgnoreCase("O=Example Co")){
							foundIssuerO = true;
							if (foundIssuerCN) break;
						}
						if(issuer.trim().equalsIgnoreCase("CN=Boeing Class 2 Windows Machines")){
							foundIssuerCN = true;
							if (foundIssuerO) break;
						}
						if(issuer.trim().equalsIgnoreCase("O=Boeing")){
							foundIssuerO = true;
							if (foundIssuerCN) break;
						}

					}
					if (!foundIssuerCN || !foundIssuerO) return false;

					//83e9c401f1f98597963ef5fbacd6840be9bf012c
					//9d4a30f85e2df3bf5ee47d9c8be85bef6815ef3b
					String certThumbPrint = getThumbprint();
					logger.info("Certificate received with thumbprint: " + certThumbPrint);
					if(!certThumbPrint.equalsIgnoreCase("9d4a30f85e2df3bf5ee47d9c8be85bef6815ef3b") && !certThumbPrint.equalsIgnoreCase("83e9c401f1f98597963ef5fbacd6840be9bf012c")){
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

	private String getThumbprint()
			throws NoSuchAlgorithmException, CertificateEncodingException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] der = cert.getEncoded();
		md.update(der);
		byte[] digest = md.digest();
		String digestHex = DatatypeConverter.printHexBinary(digest);
		return digestHex.toLowerCase();
	}


}
