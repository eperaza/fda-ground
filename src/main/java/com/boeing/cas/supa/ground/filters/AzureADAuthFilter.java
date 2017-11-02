package com.boeing.cas.supa.ground.filters;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.nimbusds.jwt.JWTParser;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AzureADAuthFilter implements Filter {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	
	@Value("#{'${list.of.apps}'.split(',')}") 
	private List<String> approvedAppId;
	
	@Value("${tenantId}")
	private String tenantId;



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
            String xAuth = httpRequest.getHeader("Authorization");
            String subKey = httpRequest.getHeader("Ocp-Apim-Subscription-Key");
            System.out.println(xAuth);
            System.out.println(subKey);
            try {
				if(isValid(xAuth)){
					chain.doFilter(request, response);
				}
			} catch (ParseException e) {
				throw new SecurityException();
			}
		}
		
        logger.info("possibly modifying the response...");
    }

	private boolean isValid(String xAuth) throws ParseException {
		boolean ret = false;
		if(xAuth == null){
        	throw new SecurityException(); 
        }else{
        	if(xAuth.contains("Bearer")){
        		xAuth = xAuth.replaceFirst("Bearer ", "");
        		Map<String, Object> claimsMap = JWTParser.parse(xAuth).getJWTClaimsSet().getClaims();
        		if(!claimsMap.get("tid").equals(tenantId)){
        			throw new SecurityException();  
        		}else if(!approvedAppId.contains(claimsMap.get("appid"))){
        			throw new SecurityException();  
        		}else if(!((Date) claimsMap.get("exp")).before(new Date())){
        			throw new SecurityException(); 
        		}else{
        			ret = true;
        		}
        	}
        }
		return ret;
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
}
