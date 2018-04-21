package com.boeing.cas.supa.ground.pojos;

import com.microsoft.aad.adal4j.AuthenticationResult;

public class AccessToken {

	protected AuthenticationResult authenticationResult;
	protected String cert;

	public AccessToken() {}

	public AccessToken(AuthenticationResult authenticationResult, String cert) {
		this.authenticationResult = authenticationResult;
		this.cert = cert;
	}

	public AuthenticationResult getAuthenticationResult() {
		return this.authenticationResult;
	}

	public void setAuthenticationResult(AuthenticationResult authenticationResult) {
		this.authenticationResult = authenticationResult;
	}

	public String getCert() {
		return this.cert;
	}

	public void setCert(String cert) {
		this.cert = cert;
	}
}
