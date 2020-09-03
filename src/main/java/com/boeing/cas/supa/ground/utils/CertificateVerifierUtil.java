package com.boeing.cas.supa.ground.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CertificateVerifierUtil {

	private final Logger logger = LoggerFactory.getLogger(CertificateVerifierUtil.class);

	@Autowired
    @Qualifier("appCertificates")
    private Map<String, X509Certificate> appCertificates;

    @Autowired
    @Qualifier("appNewCertificates")
    private Map<String, X509Certificate> appNewCertificates;

    @Autowired
    @Qualifier("appRegistrationCertificates")
    private Map<String, X509Certificate> appRegistrationCertificates;

    private X509Certificate getCertFromHeader(String certHeader) {

		logger.debug("Get certificate from header: {}", certHeader);
		X509Certificate cert = null;
		if (StringUtils.isNotBlank(certHeader)) {

			byte[] decodedBytes = Base64.getDecoder().decode(certHeader);
			try {
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(decodedBytes));
			} catch (Exception e ) {
                logger.info("Failed to extract certificate from request header: {}", e.getMessage(), e);
			}
		}

		return cert;
	}

    public boolean isValidClientCertificate(String certHeader, String certHolder, boolean registrationProcess) {

        X509Certificate x509ClientCert = this.getCertFromHeader(certHeader);
        return this.isValidClientCertificate(x509ClientCert, certHolder, registrationProcess);
    }

    public boolean isValidClientCertificate(X509Certificate x509ClientCert, String certHolder, boolean registrationProcess) {

        // Article ref: https://docs.microsoft.com/en-us/azure/app-service/app-service-web-configure-tls-mutual-auth
        try {
            if (this.isSelfSignedCert(x509ClientCert)) {
                throw new SecurityException("Self-signed certificates are not accepted");
            }
            X509Certificate x509ServerCert = null;
            X509Certificate x509NewServerCert = null;

            if (registrationProcess) {
                //one time cert
                x509ServerCert = this.appRegistrationCertificates.get(certHolder);
            } else {
                x509ServerCert = this.appCertificates.get(certHolder);
                x509NewServerCert = this.appNewCertificates.get(certHolder);
            }

            if (this.certCompare(x509ClientCert, certHolder, registrationProcess)) {
                return true;
            } else if (x509NewServerCert != null) {
                return (this.certCompare(x509NewServerCert, certHolder, registrationProcess));
            }
        }
        catch (SecurityException | NoSuchAlgorithmException | CertificateException e) {
            this.logger.error("Certificate verification failed: {}", (Object)e.getMessage());
        }
        catch (Exception e) {
            this.logger.error("Certificate verification failure: {}", (Object)e.getMessage());
        }

        return false;
    }

    /**
	 * Checks whether given X.509 certificate is valid.
	 * @param cert 
	 */
    private boolean certCompare(X509Certificate x509ClientCert, String certHolder, boolean registrationProcess) throws SecurityException {
        try {
            logger.debug("Cert Compare");
            X509Certificate x509ServerCert = this.appCertificates.get(certHolder);
            if (x509ServerCert == null) {
                logger.debug("Getting from AppNewCertificates instead");
                x509ServerCert = this.appNewCertificates.get(certHolder);
            }
            Map<String, String> x509ClientCertSubjectDn = this.getMap(x509ClientCert.getSubjectDN().getName());
            Map<String, String> x509ClientCertIssuerDn = this.getMap(x509ClientCert.getIssuerDN().getName());
            String x509ClientCertThumbPrint = this.getThumbprint(x509ClientCert);
            Map<String, String> x509ServerCertSubjectDn = this.getMap(x509ServerCert.getSubjectDN().getName());
            Map<String, String> x509ServerCertIssuerDn = this.getMap(x509ServerCert.getIssuerDN().getName());
            String x509ServerCertThumbPrint = this.getThumbprint(x509ServerCert);
            x509ClientCert.checkValidity(new Date());
            logger.debug("registration cert? {}", registrationProcess?"yes":"no");
            logger.debug("client cert subject DN: {}", x509ClientCert.getSubjectDN().getName());
            logger.debug("server cert subject DN: {}", x509ServerCert.getSubjectDN().getName());
            logger.debug("client cert issuer DN: {}", x509ClientCert.getIssuerDN().getName());
            logger.debug("server cert issuer DN: {}", x509ServerCert.getIssuerDN().getName());
            if (!x509ClientCertSubjectDn.get("CN").equals(x509ServerCertSubjectDn.get("CN"))) {
                logger.info("client cert subject (CN) = {}", x509ClientCertSubjectDn.get("CN"));
                logger.info("server cert subject (CN) = {}", x509ServerCertSubjectDn.get("CN"));
                throw new SecurityException("Subject name invalid");
            }
            if (!x509ClientCertIssuerDn.get("CN").equals(x509ServerCertIssuerDn.get("CN"))) {
                logger.info("client cert issuer (CN) = {}", x509ClientCertIssuerDn.get("CN"));
                logger.info("server cert issuer (CN) = {}", x509ServerCertIssuerDn.get("CN"));
                throw new SecurityException("Issuer name (CN) invalid");
            }
            if (!x509ClientCertIssuerDn.get("O").equals(x509ServerCertIssuerDn.get("O"))) {
                logger.info("client cert issuer (O) = {}", x509ClientCertIssuerDn.get("O"));
                logger.info("server cert issuer (O) = {}", x509ServerCertIssuerDn.get("O"));
                throw new SecurityException("Issuer name (0) invalid");
            }
            if (!x509ServerCertThumbPrint.equalsIgnoreCase(x509ClientCertThumbPrint)) {
                logger.info("client cert thumbprint = {}", x509ClientCertThumbPrint);
                logger.info("server cert thumbprint = {}", x509ServerCertThumbPrint);
                throw new SecurityException("Thumbprint mismatch");
            }
            return true;
        }
        catch (SecurityException | NoSuchAlgorithmException | CertificateException e) {
        this.logger.error("Certificate verification failed: {}", (Object)e.getMessage());
        }
        catch (Exception e) {
        this.logger.error("Certificate verification failure: {}", (Object)e.getMessage());
        }
        return false;
    }

    /**
	 * Checks whether given X.509 certificate is self-signed.
	 * @param cert 
	 */
    private boolean isSelfSignedCert(X509Certificate cert) throws CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
        try {
            PublicKey key = cert.getPublicKey();
            cert.verify(key);
            return true;
        }
        catch (SignatureException se) {
            // TODO - Figure out how to more gracefully use this?
            // logger.warn("SignatureException: {}", se.getMessage());
        }
        catch (InvalidKeyException ike) {
            logger.warn("InvalidKeyException: {}", ike.getMessage());
        }

        return false;
    }

    private String getThumbprint(X509Certificate cert) throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] der = cert.getEncoded();
        md.update(der);
        byte[] digest = md.digest();
        String digestHex = DatatypeConverter.printHexBinary(digest);
        return digestHex.toLowerCase();
    }

    private Map<String, String> getMap(String str) {

    	return Arrays.stream(str.split(","))
        			.map(item -> item.split("="))
        			.collect(Collectors.toMap(item -> item[0].trim(), item -> item[1].trim()));
    }
}
