package com.boeing.cas.supa.ground.pojos;

public class RefreshTokenOutput {

	protected String tokenType;
	protected String expiresIn;
	protected String expiresOn;
	protected String notBefore;
	protected String accessToken;
	protected String refreshToken;

	public RefreshTokenOutput() {}

	public RefreshTokenOutput(String tokenType, String expiresIn, String expiresOn, String notBefore,
			String accessToken, String refreshToken) {

		this.tokenType = tokenType;
		this.expiresIn = expiresIn;
		this.expiresOn = expiresOn;
		this.notBefore = notBefore;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
	}

	public String getTokenType() {
		return this.tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public String getExpiresIn() {
		return this.expiresIn;
	}

	public void setExpiresIn(String expiresIn) {
		this.expiresIn = expiresIn;
	}

	public String getExpiresOn() {
		return this.expiresOn;
	}

	public void setExpiresOn(String expiresOn) {
		this.expiresOn = expiresOn;
	}

	public String getNotBefore() {
		return this.notBefore;
	}

	public void setNotBefore(String notBefore) {
		this.notBefore = notBefore;
	}

	public String getAccessToken() {
		return this.accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return this.refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
}
