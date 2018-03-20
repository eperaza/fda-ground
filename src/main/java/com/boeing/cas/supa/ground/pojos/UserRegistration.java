package com.boeing.cas.supa.ground.pojos;

import com.microsoft.aad.adal4j.AuthenticationResult;

public class UserRegistration extends AccessToken {
	protected String preferences;
	protected String mobileConfig;
	public UserRegistration(AuthenticationResult authenticationResult, String cert, String preferences, String mobileConfig){
		super(authenticationResult, cert);
		this.preferences = preferences;
		this.mobileConfig = mobileConfig;
	}

	public String getPreferences() {
		return preferences;
	}

	public void setPreferences(String preferences) {
		this.preferences = preferences;
	}

	public String getMobileConfig() {
		return mobileConfig;
	}

	public void setMobileConfig(String mobileConfig) {
		this.mobileConfig = mobileConfig;
	}
	
	
}
