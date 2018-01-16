package com.boeing.cas.supa.ground.filters;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.boeing.cas.supa.ground.pojos.Error;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTParser;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AzureADAuthFilter implements Filter {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Value("#{'${list.of.apps}'.split(',')}") 
	private List<String> approvedAppId;
	
	@Value("${tenantId}")
	private String tenantId;
	
	@Value("${app.id.uri}")
	private String appIdUri;
	
	private static final Set<String> ALLOWED_PATHS = Collections.unmodifiableSet(new HashSet<>(
	        Arrays.asList("/login", "/refresh", "/logfile")));
	


	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		logger.debug("Initiating WebFilter >> ");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		logger.info("filter doing some stuff...");
		
		
		if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            printAlloftheHeader(httpRequest);
            HttpServletResponse res = (HttpServletResponse) response;
            String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length()).replaceAll("[/]+$", ""); 
            boolean allowedPath = ALLOWED_PATHS.contains(path);
            if(allowedPath){
            	chain.doFilter(request, response);
            }else {
                String xAuth = httpRequest.getHeader("Authorization");
                if(xAuth == null || xAuth.isEmpty()){
                	
                	res.setStatus(400);
  		          res.setContentType("application/json");

  		          //pass down the actual obj that exception handler normally send
  		          ObjectMapper mapper = new ObjectMapper();
  		          PrintWriter out = res.getWriter(); 
  		          out.print(mapper.writeValueAsString(new Error("Authorization_Missing", "Must provide a Authorizaiton token", System.currentTimeMillis()/1000)));
  		          out.flush();

  		          return;
                }
                try {
    				if(isValid(xAuth)){
    					chain.doFilter(request, response);
    				}
    			} catch (Exception e){
    		          //set the response object
    		          res.setStatus(400);
    		          res.setContentType("application/json");

    		          //pass down the actual obj that exception handler normally send
    		          ObjectMapper mapper = new ObjectMapper();
    		          PrintWriter out = res.getWriter(); 
    		          out.print(mapper.writeValueAsString(new Error("JWT_ERROR", e.getMessage(), System.currentTimeMillis()/1000)));
    		          out.flush();

    		          return;
    			}
            }

		}
        logger.info("possibly modifying the response...");
    }

	private void printAlloftheHeader(HttpServletRequest request) {
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			logger.info("Header Name: " + headerName);
			String headerValue = request.getHeader(headerName);
			logger.info("Header Value: " + headerValue);
		}
		String certHeader = request.getHeader("X-ARR-ClientCert");
		if(!certHeader.isEmpty() || certHeader != null){
			try{
				byte[] decodedBytes = Base64.getDecoder().decode(certHeader);
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
			    X509Certificate cert = (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(decodedBytes));
			    isValidClientCert(cert);
			    logger.info(cert.getIssuerDN().getName());
			    logger.info(cert.getNotBefore().toString());
			    logger.info(cert.getSignature().toString());
			    cert.checkValidity();
			}catch (Exception ex){
				logger.info(ex.getMessage());
			}
		}
		
		
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
    
    private boolean isValidClientCert(X509Certificate cert){
    	boolean isValid = false;
    	try {
			boolean isSelfSignedCert = isSelfSignedCert(cert);
			logger.info(isSelfSignedCert ? "True" : "False");
		} catch (CertificateException | NoSuchAlgorithmException | NoSuchProviderException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	if(cert != null){
    		//1. The certificate is not expired and is active for the current time on server.
    		try {
				cert.checkValidity(new Date());
			} catch (CertificateExpiredException | CertificateNotYetValidException e) {
				logger.info(e.getMessage());
				isValid = false;
			}
    		//2. The subject name of the certificate has the common name nildevecc
    		Principal principal = cert.getSubjectDN();
            String subjectDn = principal.getName();
            logger.info("subjectDn: " +subjectDn);
            
            principal = cert.getIssuerDN();
            String issuerDn = principal.getName();
            logger.info("ISSUER: " + issuerDn);
            
            try {
				String thumbPrint = getThumbprint(cert);
				logger.info(thumbPrint);
			} catch (CertificateEncodingException | NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
    	}
    	return isValid;
    	
    }
	
	private boolean isValid(String xAuth) throws ParseException, SecurityException {
		boolean ret = false;
		if(xAuth == null || xAuth.isEmpty()){
        	throw new SecurityException("Not a valid Authorizaiton token"); 
        }else{
        	if(xAuth.contains("Bearer")){
        		xAuth = xAuth.replaceFirst("Bearer ", "");
        		Map<String, Object> claimsMap = JWTParser.parse(xAuth).getJWTClaimsSet().getClaims();
        		if(claimsMap.get("aud") instanceof List<?>){
        			Set<String> set = new HashSet<String>((Collection<? extends String>) claimsMap.get("aud"));
        			logger.info(set.toString());
        			if(!set.contains(appIdUri)){
        				
        				logger.info("Aud doesn't exist");
        				throw new SecurityException("Not a valid Authorizaiton token: Aud doesn't exist");  
        			}
        		}
        		if(!claimsMap.get("tid").equals(tenantId)){
        			logger.info("TenantId doesn't exist");
        			throw new SecurityException("Not a valid Authorizaiton token: TenantId doesn't exist");  
        		}
        		if(!approvedAppId.contains(claimsMap.get("appid"))){
        			logger.info("Not part of approved apps");
        			throw new SecurityException("Not a valid Authorizaiton token: Not part of approved apps");  
        		}
        		logger.info(claimsMap.get("exp").getClass().getName());
        		
        		if(!((Date) claimsMap.get("exp")).after(new Date())){
        			logger.info("Expired token");
        			throw new SecurityException("Not a valid Authorizaiton token: expired token"); 
        		}
        		logger.info("everything passed...");
        		ret = true;
        	}else{
        		throw new SecurityException("Not a valid Authorizaiton token"); 
        	}
        }
		return ret;
	}
	
	private static String getThumbprint(X509Certificate cert)
            throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] der = cert.getEncoded();
        md.update(der);
        byte[] digest = md.digest();
        String digestHex = DatatypeConverter.printHexBinary(digest);
        return digestHex.toLowerCase();
    }

	@Override
	public void destroy() {
		throw new UnsupportedOperationException();
	}
}
