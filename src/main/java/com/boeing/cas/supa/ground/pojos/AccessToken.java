package com.boeing.cas.supa.ground.pojos;

import java.util.List;

import com.microsoft.aad.adal4j.AuthenticationResult;

public class AccessToken {

	private AuthenticationResult authenticationResult;
	private String airline;
	private List<String> roles;

	public AccessToken() {}

	public AccessToken(AuthenticationResult authenticationResult, String airline, List<String> roles) {
		this.authenticationResult = authenticationResult;
		this.airline = airline;
		this.roles = roles;
	}

	public AuthenticationResult getAuthenticationResult() {
		return this.authenticationResult;
	}

	public void setAuthenticationResult(AuthenticationResult authenticationResult) {
		this.authenticationResult = authenticationResult;
	}

	public String getAirline() {
		return airline;
	}

	public void setAirline(String airline) {
		this.airline = airline;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
}
