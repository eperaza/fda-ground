package com.boeing.cas.supa.ground.pojos;

import java.util.List;

import com.microsoft.aad.adal4j.AuthenticationResult;

public class UserRegistration extends AccessToken {

	protected String preferences;
	protected String mobileConfig;

	public UserRegistration(AuthenticationResult authenticationResult, String airline, List<String> roles,
			String preferences, String mobileConfig) {

		super(authenticationResult, airline, roles);
		this.preferences = preferences;
		this.mobileConfig = mobileConfig;
	}

	public String getPreferences() {
		return this.preferences;
	}

	public void setPreferences(String preferences) {
		this.preferences = preferences;
	}

	public String getMobileConfig() {
		return this.mobileConfig;
	}

	public void setMobileConfig(String mobileConfig) {
		this.mobileConfig = mobileConfig;
	}
}
