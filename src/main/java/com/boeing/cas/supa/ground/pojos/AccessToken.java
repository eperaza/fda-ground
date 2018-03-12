package com.boeing.cas.supa.ground.pojos;

import com.microsoft.aad.adal4j.AuthenticationResult;

public class AccessToken {
	protected AuthenticationResult authenticationResult;
	protected String cert;
	public AuthenticationResult getAuthenticationResult() {
		return authenticationResult;
	}
	public void setAuthenticationResult(AuthenticationResult authenticationResult) {
		this.authenticationResult = authenticationResult;
	}
	public String getCert() {
		return cert;
	}
	public void setCert(String cert) {
		this.cert = cert;
	}
	public AccessToken(AuthenticationResult authenticationResult, String cert) {
		this.authenticationResult = authenticationResult;
		this.cert = cert;
	}
	public AccessToken(){
		this(null, null);
	}
	
}
