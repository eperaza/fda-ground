package com.boeing.cas.supa.ground.pojos;

public class RefreshTokenOutput {
	protected String token_type;
	protected String expires_in;
	protected String expires_on;
	protected String not_before;
	protected String access_token;
	protected String refresh_token;
	
	@SuppressWarnings("unused")
	private RefreshTokenOutput(){
		// private constructor hides implicit public one
	}
	public RefreshTokenOutput(String token_type, String expires_in, String expires_on, String not_before,
			String access_token, String refresh_token) {
		this.token_type = token_type;
		this.expires_in = expires_in;
		this.expires_on = expires_on;
		this.not_before = not_before;
		this.access_token = access_token;
		this.refresh_token = refresh_token;
	}
	public String getToken_type() {
		return token_type;
	}
	public String getExpires_in() {
		return expires_in;
	}
	public String getExpires_on() {
		return expires_on;
	}
	public String getNot_before() {
		return not_before;
	}
	public String getAccess_token() {
		return access_token;
	}
	public String getRefresh_token() {
		return refresh_token;
	}
	public void setToken_type(String token_type) {
		this.token_type = token_type;
	}
	public void setExpires_in(String expires_in) {
		this.expires_in = expires_in;
	}
	public void setExpires_on(String expires_on) {
		this.expires_on = expires_on;
	}
	public void setNot_before(String not_before) {
		this.not_before = not_before;
	}
	public void setAccess_token(String access_token) {
		this.access_token = access_token;
	}
	public void setRefresh_token(String refresh_token) {
		this.refresh_token = refresh_token;
	}
	
}
