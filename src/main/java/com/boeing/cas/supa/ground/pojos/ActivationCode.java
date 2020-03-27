package com.boeing.cas.supa.ground.pojos;


public class ActivationCode {

	private String emailAddress;
	private String activationCode;
	private String registrationCert;
	private String airline;

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getActivationCode() {
		return activationCode;
	}

	public void setActivationCode(String activationCode) {
		this.activationCode = activationCode;
	}

	public String getRegistrationCert() {
		return registrationCert;
	}

	public void setRegistrationCert(String registrationCert) {
		this.registrationCert = registrationCert;
	}

	public String getAirline() {
		return airline;
	}

	public void setAirline(String airline) {
		this.airline = airline;
	}

	@Override
	public String toString() {
		return "ActivationCode{" +
				"emailAddress='" + emailAddress + '\'' +
				"activationCode='" + activationCode + '\'' +
				", registrationCert='" + registrationCert + '\'' +
				", airline='" + airline + '\'' +
				'}';
	}
}

