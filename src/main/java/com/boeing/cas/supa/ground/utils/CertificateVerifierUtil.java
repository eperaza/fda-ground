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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.boeing.cas.supa.ground.pojos.KeyVaultProperties;

@Component
@EnableConfigurationProperties(KeyVaultProperties.class)
public class CertificateVerifierUtil {

	private final Logger logger = LoggerFactory.getLogger(CertificateVerifierUtil.class);

	@Autowired
    private KeyVaultProperties keyVaultProperties;

	private X509Certificate getCertFromHeader(String certHeader) {

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

    public boolean isValidClientCertificate(String certHeader, String certHolder) {

    	X509Certificate x509ClientCert = this.getCertFromHeader(certHeader);
        return this.isValidClientCertificate(x509ClientCert, certHolder);
    }

	public boolean isValidClientCertificate(X509Certificate x509ClientCert, String certHolder) {

		// Article ref: https://docs.microsoft.com/en-us/azure/app-service/app-service-web-configure-tls-mutual-auth
		try {
		
			if (this.isSelfSignedCert(x509ClientCert)) {
				throw new SecurityException("Self-signed certificates are not accepted");
			}

            KeyVaultRetriever kvr = new KeyVaultRetriever(this.keyVaultProperties.getClientId(), this.keyVaultProperties.getClientKey());
            X509Certificate x509ServerCert = kvr.getCertificateByCertName(certHolder);
            Map<String, String> x509ClientCertSubjectDn = this.getMap(x509ClientCert.getSubjectDN().getName());
            Map<String, String> x509ClientCertIssuerDn = this.getMap(x509ClientCert.getIssuerDN().getName());
            String x509ClientCertThumbPrint = this.getThumbprint(x509ClientCert);
            Map<String, String> x509ServerCertSubjectDn = this.getMap(x509ServerCert.getSubjectDN().getName());
            Map<String, String> x509ServerCertIssuerDn = this.getMap(x509ServerCert.getIssuerDN().getName());
            String x509ServerCertThumbPrint = this.getThumbprint(x509ServerCert);
            x509ClientCert.checkValidity(new Date());
            if (!x509ClientCertSubjectDn.get("CN").equals(x509ServerCertSubjectDn.get("CN"))) {
                throw new SecurityException("Subject name invalid");
            }
            if (!x509ClientCertIssuerDn.get("CN").equals(x509ServerCertIssuerDn.get("CN"))) {
                throw new SecurityException("Issuer name (CN) invalid");
            }
            if (!x509ClientCertIssuerDn.get("O").equals(x509ServerCertIssuerDn.get("O"))) {
                throw new SecurityException("Issuer name (0) invalid");
            }
            if (!x509ServerCertThumbPrint.equalsIgnoreCase(x509ClientCertThumbPrint)) {
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
            logger.warn("SignatureException: {}", se.getMessage());
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
