package com.boeing.cas.supa.ground.pojos;

public class UserAccountRegistration {

	private String registrationToken;
	private String userObjectId;
	private String userPrincipalName;
	private String airline;
	private String workEmail;
	private String accountState;

    public UserAccountRegistration() {}

    public UserAccountRegistration(String registrationToken, String userObjectId, String userPrincipalName,
    		String airline, String workEmail, String accountState) {

    	this.registrationToken = registrationToken;
    	this.userObjectId = userObjectId;
    	this.userPrincipalName = userPrincipalName;
    	this.airline = airline;
    	this.workEmail = workEmail;
    	this.accountState = accountState;
    }

    public String getRegistrationToken() {
		return registrationToken;
	}

	public void setRegistrationToken(String registrationToken) {
		this.registrationToken = registrationToken;
	}

	public String getUserObjectId() {
		return userObjectId;
	}

	public void setUserObjectId(String userObjectId) {
		this.userObjectId = userObjectId;
	}

	public String getUserPrincipalName() {
		return userPrincipalName;
	}

	public void setUserPrincipalName(String userPrincipalName) {
		this.userPrincipalName = userPrincipalName;
	}

	public String getAirline() {
		return airline;
	}

	public void setAirline(String airline) {
		this.airline = airline;
	}

	public String getWorkEmail() {
		return workEmail;
	}

	public void setWorkEmail(String workEmail) {
		this.workEmail = workEmail;
	}

	public String getAccountState() {
		return accountState;
	}

	public void setAccountState(String accountState) {
		this.accountState = accountState;
	}

	@Override
	public String toString() {
		
		return new StringBuilder('[').append(this.getClass().getSimpleName()).append(']').append(':')
				.append(this.registrationToken).append(',')
				.append(this.userObjectId).append(',')
				.append(this.userPrincipalName).append(',')
				.append(this.airline).append(',')
				.append(this.workEmail).append(',')
				.append(this.accountState)
				.append(']')
			.toString();
	}
}