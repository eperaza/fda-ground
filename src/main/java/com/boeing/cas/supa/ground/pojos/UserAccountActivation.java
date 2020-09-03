package com.boeing.cas.supa.ground.pojos;

public class UserAccountActivation {

	private String registrationToken;
	private String username;
	private String password;
	private String activationCode = "unknown";

	public UserAccountActivation() {}
	
	public UserAccountActivation(String registrationToken, String username, String password) {

		this.registrationToken = registrationToken;
		this.username = username;
		this.password = password;
	}

	public UserAccountActivation(String registrationToken, String username, String password, String activationCode) {

		this.registrationToken = registrationToken;
		this.username = username;
		this.password = password;
		this.activationCode = activationCode;
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

	public String getActivationCode() {
		return activationCode;
	}

	public void setActivationCode(String activationCode) {
		this.activationCode = activationCode;
	}

	@Override
	public String toString() {

		return new StringBuilder("[").append(this.getClass().getSimpleName()).append(']').append(':')
				.append(this.registrationToken).append(',')
				.append(this.username).append(',')
				.append(this.password).append(',')
				.append(this.activationCode)
				.append(']')
			.toString();
	}
}
