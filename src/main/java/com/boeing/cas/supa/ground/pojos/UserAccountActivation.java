package com.boeing.cas.supa.ground.pojos;

public class UserAccountActivation {

	private String registrationToken;
	private String username;
	private String password;

	public UserAccountActivation() {}
	
	public UserAccountActivation(String registrationToken, String username, String password) {
		super();
		this.registrationToken = registrationToken;
		this.username = username;
		this.password = password;
	}

	public String getRegistrationToken() {
		return registrationToken;
	}

	public void setRegistrationToken(String registrationToken) {
		this.registrationToken = registrationToken;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public String toString() {

		return new StringBuilder('[').append(this.getClass().getSimpleName()).append(']').append(':')
				.append(this.registrationToken).append(',')
				.append(this.username).append(',')
				.append(this.password)
				.append(']')
			.toString();
	}
}
