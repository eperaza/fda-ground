package com.boeing.cas.supa.ground.filters;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.boeing.cas.supa.ground.pojos.Error;
import com.boeing.cas.supa.ground.utils.CertificateVerifierUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTParser;

//@Component
//@Order(Ordered.HIGHEST_PRECEDENCE)
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
			HttpServletResponse httpResponse = (HttpServletResponse) response;
			String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length()).replaceAll("[/]+$", ""); 
			boolean allowedPath = ALLOWED_PATHS.contains(path);
			
			if(allowedPath){
				//Only allow client1 and mihir certs
				if(!isValidClientCert("client1", httpRequest)){
					httpResponse.setStatus(403);
					httpResponse.setContentType("application/json");

					//pass down the actual obj that exception handler normally send
					ObjectMapper mapper = new ObjectMapper();
					PrintWriter out = httpResponse.getWriter(); 
					out.print(mapper.writeValueAsString(new Error("certificate missing", "Must provide a valid client certificate")));
					out.flush();
					return;
				}else{
					chain.doFilter(request, response);
				}
				
			}else{
				if(!isValidClientCert("client2", httpRequest)){
					httpResponse.setStatus(403);
					httpResponse.setContentType("application/json");

					//pass down the actual obj that exception handler normally send
					ObjectMapper mapper = new ObjectMapper();
					PrintWriter out = httpResponse.getWriter(); 
					out.print(mapper.writeValueAsString(new Error("Invalid client certificate", "Must provide a valid client certificate")));
					out.flush();
					return;
				}else{
					String xAuth = httpRequest.getHeader("Authorization");
					if(xAuth == null || xAuth.isEmpty()){

						httpResponse.setStatus(400);
						httpResponse.setContentType("application/json");

						//pass down the actual obj that exception handler normally send
						ObjectMapper mapper = new ObjectMapper();
						PrintWriter out = httpResponse.getWriter(); 
						out.print(mapper.writeValueAsString(new Error("Authorization_Missing", "Must provide a Authorizaiton token")));
						out.flush();

						return;
					}
					try {
						if(isValidOAuthToken(xAuth)){
							chain.doFilter(request, response);
						}
					} catch (Exception e){
						//set the response object
						httpResponse.setStatus(400);
						httpResponse.setContentType("application/json");

						//pass down the actual obj that exception handler normally send
						ObjectMapper mapper = new ObjectMapper();
						PrintWriter out = httpResponse.getWriter(); 
						out.print(mapper.writeValueAsString(new Error("JWT_ERROR", e.getMessage())));
						out.flush();

						return;
					}
					
				}
			}
		}
		logger.info("possibly modifying the response...");
	}

	private boolean isValidClientCert(String certHolder, HttpServletRequest httpRequest) {
		String certHeader = httpRequest.getHeader("X-ARR-ClientCert");
		boolean isValid = false;
		if(!certHeader.isEmpty() || certHeader != null){
			CertificateVerifierUtil cvu = new CertificateVerifierUtil(certHeader, certHolder);
			isValid = cvu.IsValidClientCertificate();
		}
		return isValid;
		
	}

	private boolean isValidOAuthToken(String xAuth) throws ParseException, SecurityException {
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



	@Override
	public void destroy() {
		throw new UnsupportedOperationException();
	}
}
