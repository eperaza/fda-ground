package com.boeing.cas.supa.ground.pojos;

public class RefreshTokenInput {

	protected String refreshToken;

	public RefreshTokenInput() {}

	public RefreshTokenInput(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getRefreshToken() {
		return this.refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
}
