package com.boeing.cas.supa.ground.filters;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.utils.CertificateVerifierUtil;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.ControllerUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTParser;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AzureADAuthFilter implements Filter {

	private final Logger logger = LoggerFactory.getLogger(AzureADAuthFilter.class);

	@Value("${api.azuread.uri}")
	private String azureadApiUri;

	@Autowired
	private Map<String, String> appProps;

	private static final Set<String> ALLOWED_PATHS = Collections.unmodifiableSet(
			new HashSet<>(
					Arrays.asList("/login", "/refresh", "/register", "/registeruser", "/logfile", "/error" )));

	@Autowired
	private CertificateVerifierUtil certVerify;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		logger.debug("Initiating AzureADAuthFilter...");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		if (!(request instanceof HttpServletRequest)) {
			return;
		}

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length()).replaceAll("[/]+$", StringUtils.EMPTY);
		logger.debug("AzureADAuthFilter intercepting request: {}", ControllerUtils.sanitizeString(path));
		boolean allowedPath = ALLOWED_PATHS.contains(path);

		int responseCode = 400;
		ApiError responseException = null;

		if (allowedPath) {

			if (this.isValidClientCertInReqHeader("client1", httpRequest)) {
				logger.debug("Client1 cert is valid, moving request along");
				chain.doFilter(request, response);
				return;
			}

			responseCode = 403;
			responseException = new ApiError("certificate missing", "Must provide a valid client certificate");
			sendResponse(responseCode, responseException, httpResponse);
			return;
		}

		try {

			logger.debug("Checking Client2 cert and OAuth2 token...");
			boolean validClientCert = this.isValidClientCertInReqHeader("client2", httpRequest);
			boolean validOAuthToken = this.isValidOAuthToken(httpRequest.getHeader("Authorization"));
			if (validClientCert && validOAuthToken) {
				logger.debug("Client2 cert and OAuth2 token are good!");
                chain.doFilter(request, response);
                return;
            }
            else if (!validClientCert) {
				logger.error("Client2 cert failed!");
            	responseCode = 403;
                responseException = new ApiError("Invalid client certificate", "Must provide a valid client certificate");
            }
            else {
				logger.error("OAuth2 token failed!");
            	responseCode = 401;
                responseException = new ApiError("Authorization_Missing", "Must provide a valid authorization token");
            }
        }
        catch (ParseException pe) {
            responseCode = 400;
            responseException = new ApiError("JWT_ERROR", pe.getMessage());
        }
        catch (SecurityException se) {
        	responseCode = 401;
        	responseException = new ApiError("MISSING_INVALID_CLIENT_AUTH", se.getMessage());
        }
        finally {

            if (responseException != null) {
            	sendResponse(responseCode, responseException, httpResponse);
            }
        }
	}

	private void sendResponse(int responseCode, ApiError responseException, HttpServletResponse httpResponse) throws IOException {

		logger.debug("Sending response: ", responseException != null ? responseException.toString() : StringUtils.EMPTY);
		httpResponse.setStatus(responseCode);
		httpResponse.setContentType("application/json");
        ObjectMapper mapper = new ObjectMapper();
        PrintWriter out = httpResponse.getWriter();
        out.print(mapper.writeValueAsString(responseException));
        out.flush();
	}
	
	private boolean isValidClientCertInReqHeader(String certHolder, HttpServletRequest httpRequest) {

    	String certHeader = httpRequest.getHeader("X-ARR-ClientCert");
        if (StringUtils.isNotBlank(certHeader)) {
            return this.certVerify.isValidClientCertificate(certHeader, certHolder);
        }

        return this.isValidClientCertInReqAttribute(certHolder, httpRequest);
    }

	private boolean isValidClientCertInReqAttribute(String certHolder, HttpServletRequest httpRequest) {

		X509Certificate[] certs = (X509Certificate[]) httpRequest.getAttribute("javax.servlet.request.X509Certificate");
		boolean isValid = false;

		if (ArrayUtils.isNotEmpty(certs)) {

			logger.debug("certs of length: {}", certs.length);
			X509Certificate clientCert = certs[0];
			logger.debug("cert properties: {}", clientCert.toString());
			logger.debug("cert subject: {}", clientCert.getSubjectDN().getName());
			logger.debug("cert issuer: {}", clientCert.getIssuerDN().getName());
			isValid = certVerify.isValidClientCertificate(clientCert, certHolder);
		}

		return isValid;
	}

	private boolean isValidOAuthToken(String xAuth) throws ParseException {

		logger.debug("xAuth token -> {}", xAuth);
		if (StringUtils.isBlank(xAuth) || !xAuth.contains(Constants.AUTH_HEADER_PREFIX.trim())) {
			throw new SecurityException("Missing or invalid Authorization token");
		}

		Map<String, Object> claimsMap = JWTParser.parse(xAuth.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY)).getJWTClaimsSet().getClaims();
		if (claimsMap.get("aud") instanceof List<?>) {

			Set<String> set = ((List<?>) claimsMap.get("aud"))
								.stream()
								.filter(Objects::nonNull)
								.map(Object::toString)
								.collect(Collectors.toSet());
			logger.debug("aud Claims: {}", set.toString());
			if (!set.contains(this.azureadApiUri)) {
				logger.error("Aud does not exist");
				throw new SecurityException("Not a valid Authorization token: Aud does not exist");
			}
		}

        if (!claimsMap.get("tid").equals(this.appProps.get("AzureADTenantID"))) {
            logger.error("TenantId doesn't exist");
            throw new SecurityException("Not a valid Authorization token: TenantId doesn't exist");
        }
        if (!this.appProps.get("AzureADAppClientID").equals(claimsMap.get("appid"))) {
            logger.error("Not part of approved apps");
            throw new SecurityException("Not a valid Authorization token: Not part of approved apps");
        }
        if (!((Date) claimsMap.get("exp")).after(new Date())) {
            logger.error("Expired token");
            throw new SecurityException("Not a valid Authorization token: expired token");
        }
        logger.debug("All checks on the OAuth2 token passed");

        return true;
	}

	@Override
	public void destroy() {
		throw new UnsupportedOperationException();
	}
}
