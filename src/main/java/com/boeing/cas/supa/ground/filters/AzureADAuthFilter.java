package com.boeing.cas.supa.ground.filters;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.boeing.cas.supa.ground.exceptions.UserAccountRegistrationException;
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
import com.boeing.cas.supa.ground.dao.UserAccountRegistrationDao;
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

	@Autowired
	private UserAccountRegistrationDao userAccountRegister;

	private static final Set<String> ALLOWED_PATHS = Collections.unmodifiableSet(
			new HashSet<>(
					Arrays.asList("/login", "/refresh", "/register", "/registeruser", "/logfile", "/getregistrationcode", "/error", "/createnewusers" )));

	// include any path which uses the (one-time) registration cert
	private static final Set<String> REGISTRATION_PATH = Collections.unmodifiableSet(
			new HashSet<>(Arrays.asList("/getclientcert")));

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

		boolean registrationPath = REGISTRATION_PATH.contains(path);
		logger.debug("registration path? {}", registrationPath?"yes":"no");

		int responseCode = 400;
		ApiError responseException = null;

		//need to remove hard-coded names, and use keyvault entries instead
		if (registrationPath) {
			if (this.isValidClientCertInReqHeader("fdadvisor2z", httpRequest, true)) {
				logger.debug("{} cert is valid, moving request along", "fdadvisor2z");
				chain.doFilter(request, response);
				return;
			}
			logger.debug("registration client cert is missing or not valid.");
			responseCode = 403;
			responseException = new ApiError("registration certificate missing", "Must provide a valid client registration certificate");
			sendResponse(responseCode, responseException, httpResponse);
			return;
		}
		boolean isUsingPrimaryCert = this.isValidClientCertInReqHeader(appProps.get("FDAdvisorClientCertName"), httpRequest, false);
		boolean isUsingSecondaryCert = this.isValidClientCertInReqHeader(appProps.get("FDAdvisor1ClientCertName"), httpRequest, false);

		Object base64EncodedPayload = null;
		if (allowedPath) {
			Object result = null;
			if (isUsingPrimaryCert || isUsingSecondaryCert) {

				logger.debug("{} cert is valid, moving request along", appProps.get("FDAdvisorClientCertName"));
				chain.doFilter(request, response);
				return;
			} 

			responseCode = 403;
			responseException = new ApiError("certificate missing", "Must provide a valid client certificate");
			sendResponse(responseCode, responseException, httpResponse);
			return;
		}

		try {

			logger.debug("Checking {} cert and OAuth2 token...", appProps.get("FDAdvisorClientCertName"));
			boolean validClientCert = isUsingPrimaryCert || isUsingSecondaryCert;
			boolean validOAuthToken = this.isValidOAuthToken(httpRequest.getHeader("Authorization"));
			if (validClientCert && validOAuthToken) {
				logger.debug("{} cert and OAuth2 token are good!", appProps.get("FDAdvisorClientCertName"));
				chain.doFilter(request, response);
				return;
			}
			else if (!validClientCert) {
				logger.error("{} cert failed!", appProps.get("FDAdvisorClientCertName"));
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
			logger.error("Parse exception while verifying cert and/or OAuth2 token: {}", pe.getMessage(), pe);
			responseException = new ApiError("JWT_ERROR", pe.getMessage());
		}
		catch (SecurityException se) {
			responseCode = 401;
			// If request URL corresponds to root of application, a monitoring/ping service may be behind
			// the calls. In that case, do not show the stack trace and categorize this as a warning.
			if (path.equals("/")
					|| path.equals(StringUtils.EMPTY)) {
				logger.warn("Security exception while verifying cert and/or OAuth2 token: {}", se.getMessage());
			} else {
				logger.error("Security exception while verifying cert and/or OAuth2 token for request URL [{}]: {}", path, se.getMessage(), se);
			}
			responseException = new ApiError("MISSING_INVALID_CLIENT_AUTH", se.getMessage());
		}
		finally {

			if (responseException != null) {
				sendResponse(responseCode, responseException, httpResponse);
			}
		}
	}

	private void sendResponse(int responseCode, ApiError responseException, HttpServletResponse httpResponse) throws IOException {

		if (responseException != null) {
			logger.error("ApiError://Label: {}, Description: {}", responseException.getErrorLabel(), responseException.getErrorDescription());
		} else {
			logger.debug("Sending response with response code {}", responseCode);
		}
		httpResponse.setStatus(responseCode);
		httpResponse.setContentType("application/json");
        ObjectMapper mapper = new ObjectMapper();
        PrintWriter out = httpResponse.getWriter();
        out.print(mapper.writeValueAsString(responseException));
        out.flush();
	}

	private boolean isValidClientCertInReqHeader(String certHolder, HttpServletRequest httpRequest,
												 boolean registrationProcess) {

		String certHeader = httpRequest.getHeader("X-ARR-ClientCert");
		logger.debug("registration cert? {}", registrationProcess?"yes":"no");
		logger.debug("certificate header in request: {}", certHeader);
		if (StringUtils.isNotBlank(certHeader)) {
			return this.certVerify.isValidClientCertificate(certHeader, certHolder, registrationProcess);
		}

		return this.isValidClientCertInReqAttribute(certHolder, httpRequest, registrationProcess);
	}

	private boolean isValidClientCertInReqAttribute(String certHolder, HttpServletRequest httpRequest,
													boolean registrationProcess) {

		logger.debug("certificate in request as attribute: {}", certHolder);
		X509Certificate[] certs = (X509Certificate[]) httpRequest.getAttribute("javax.servlet.request.X509Certificate");
		boolean isValid = false;

		if (ArrayUtils.isNotEmpty(certs)) {

			logger.debug("registration cert? {}", registrationProcess?"yes":"no");
			logger.debug("certs of length: {}", certs.length);
			X509Certificate clientCert = certs[0];
			logger.debug("cert properties: {}", clientCert.toString());
			logger.debug("cert subject: {}", clientCert.getSubjectDN().getName());
			logger.debug("cert issuer: {}", clientCert.getIssuerDN().getName());
			isValid = certVerify.isValidClientCertificate(clientCert, certHolder, registrationProcess);
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
