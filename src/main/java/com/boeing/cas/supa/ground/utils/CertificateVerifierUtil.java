package com.boeing.cas.supa.ground.utils;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Properties;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class CertificateVerifierUtil {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private X509Certificate cert;
	private String certHolder;
	private static Properties certProp;
    
    static{
        InputStream is = null;
        try {
        	certProp = new Properties();
            is = CertificateVerifierUtil.class.getClassLoader().getResourceAsStream("Certificate.properties");;
            certProp.load(is);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
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
					String subjectDn = cert.getSubjectDN().getName();
					String issuerDn = cert.getIssuerDN().getName();
					String certThumbPrint = getThumbprint();
					 
					//1. The certificate is not expired and is active for the current time on server.
					cert.checkValidity(new Date());

					//2. The subject name of the certificate has the common name nildevecc					
					
					logger.info("Certificate received with subjectDn: " +subjectDn);
					boolean foundSubject = false;
					List<String> subjectDnList = Arrays.asList(subjectDn.split(","));
					String clientHolderSubjectCN = getPropertyValue(certHolder+".subject.CN");
					for(String subject: subjectDnList){
						if(subject.trim().equalsIgnoreCase("CN="+clientHolderSubjectCN)){
							foundSubject = true;
							break;
						}
					}




					//3. Check issuer name of certificate
					if (!foundSubject) return false;
					List<String> issuerDnList = Arrays.asList(issuerDn.split(","));
					logger.info("Certificate received with issuerDn: " +issuerDn);
					boolean foundIssuerCN = false, foundIssuerO = false;
					String clientHolderIssuerCN = getPropertyValue(certHolder+".issuer.CN");
					String clientHolderIssuerO = getPropertyValue(certHolder+".issuer.O");
					for(String issuer: issuerDnList){
						if(issuer.trim().equalsIgnoreCase("CN="+clientHolderIssuerCN)){
							foundIssuerCN = true;
							if (foundIssuerO) break;
						}
						if(issuer.trim().equalsIgnoreCase("O="+clientHolderIssuerO)){
							foundIssuerO = true;
							if (foundIssuerCN) break;
						}

					}
					if (!foundIssuerCN || !foundIssuerO) return false;
					
					logger.info("Certificate received with thumbprint: " + certThumbPrint);
					String clientHolderThumbPrint = getPropertyValue(certHolder+".thumbprint");
					if(!certThumbPrint.equalsIgnoreCase(clientHolderThumbPrint)){
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
	
	public static String getPropertyValue(String key){
        return certProp.getProperty(key);
    }


}
