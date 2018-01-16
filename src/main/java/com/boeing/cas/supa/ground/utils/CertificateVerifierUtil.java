package com.boeing.cas.supa.ground.utils;

import java.io.ByteArrayInputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;

public class CertificateVerifierUtil {
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
			} catch (CertificateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return cert;
	}
	private boolean IsValidClientCertificate(){
		boolean isValid = false;
		if(cert != null){
			try {
				boolean isSelfSignedCert = isSelfSignedCert(cert);
				if(!isSelfSignedCert){
					//1. The certificate is not expired and is active for the current time on server.
					cert.checkValidity(new Date());
					
					//2. The subject name of the certificate has the common name nildevecc
					//subjectDn: CN=a5605027.mw.nos.boeing.com
		    		Principal principal = cert.getSubjectDN();
		            String subjectDn = principal.getName();
		            boolean foundSubject = false;
		            List<String> subjectDnList = Arrays.asList(subjectDn.split(","));
		            for(String subject: subjectDnList){
		            	if(subject.trim().equalsIgnoreCase("CN=nildevecc")){
		            		foundSubject = true;
		            		break;
		            	}
		            }
		            
		            //3. Check issuer name of certificate
		            //CN=Boeing Class 2 Windows Machines, OU=certservers, O=Boeing, C=US
		            if (!foundSubject) return false;
		            
		            principal = cert.getIssuerDN();
		            String issuerDn = principal.getName();
		            List<String> issuerDnList = Arrays.asList(issuerDn.split(","));
		            boolean foundIssuerCN = false, foundIssuerO = false;
		            for(String issuer: issuerDnList){
		            	if(issuer.trim().equalsIgnoreCase("CN=nildevecc")){
		            		foundIssuerCN = true;
		            		if (foundIssuerO) break;
		            	}
		            	if(issuer.trim().equalsIgnoreCase("O=Microsoft Corp")){
		            		foundIssuerO = true;
		            		if (foundIssuerCN) break;
		            	}
		            }
		            if (!foundIssuerCN || !foundIssuerO) return false;
		            
				}
			} catch (CertificateException | NoSuchAlgorithmException | NoSuchProviderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return isValid;
		
	}
    /**
     * Checks whether given X.509 certificate is self-signed.
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
	

}
