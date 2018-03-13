package com.boeing.cas.supa.ground.pojos;

import com.microsoft.aad.adal4j.AuthenticationResult;

public class UserRegistration extends AccessToken {
	protected String preferences;
	
	public UserRegistration(AuthenticationResult authenticationResult, String cert, String preferences){
		super(authenticationResult, cert);
		this.preferences = preferences;
	}

	public String getPreferences() {
		return preferences;
	}

	public void setPreferences(String preferences) {
		this.preferences = preferences;
	}
	

}
