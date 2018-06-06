package com.boeing.cas.supa.ground.pojos;

import org.apache.commons.lang3.StringUtils;

public class Credential {

	protected String azUsername;
	protected String azPassword;

	public Credential() {}

	public Credential(String azUsername, String azPassword) {
		this.azUsername = azUsername;
		this.azPassword = azPassword;
	}

	public String getAzUsername() {
		return this.azUsername;
	}

	public void setAzUsername(String azUsername) {
		this.azUsername = azUsername;
	}

	public String getAzPassword() {
		return this.azPassword;
	}

	public void setAzPassword(String azPassword) {
		this.azPassword = azPassword;
	}

	public boolean isValid() {
		return StringUtils.isNotBlank(this.getAzUsername()) && StringUtils.isNotBlank(this.getAzPassword());
	}
}
