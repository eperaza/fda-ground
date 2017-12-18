package com.boeing.cas.supa.ground.pojos;

public class RefreshTokenInput {
	protected String refreshToken;
	
	private RefreshTokenInput(){
		// private constructor hides implicit public one
	}
	/**
	 * @param userInfo
	 * @param refreshToken
	 */
	public RefreshTokenInput(String userInfo, String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
	
}
