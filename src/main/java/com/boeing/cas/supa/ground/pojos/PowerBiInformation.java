package com.boeing.cas.supa.ground.pojos;

public class PowerBiInformation {

	private String airline;
	private String workspaceId;
	private String reportId;
	private String accessToken;
	private String accessExpiration;
	private String embeddedToken;
	private String embeddedExpiration;

	public PowerBiInformation (String airline, String workspaceId, String reportId) {
		this.airline = airline;
		this.workspaceId = workspaceId;
		this.reportId = reportId;
	}

	public PowerBiInformation (String airline, String workspaceId, String reportId, String accessToken,
			   String accessExpiration, String embeddedToken, String embeddedExpiration) {
		this.airline = airline;
		this.workspaceId = workspaceId;
		this.reportId = reportId;
		this.accessToken = accessToken;
		this.accessExpiration = accessExpiration;
		this.embeddedToken = embeddedToken;
		this.embeddedExpiration = embeddedExpiration;
	}

	public String getAirline() {
		return this.airline;
	}

	public void setAirline(String airline) {
		this.airline = airline;
	}

	public String getWorkspaceId() {
		return this.workspaceId;
	}

	public void setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
	}

	public String getReportId() {
		return this.reportId;
	}

	public void setReportId(String reportId) {
		this.reportId = reportId;
	}

	public String getAccessToken() {
		return this.accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getAccessExpiration() {
		return this.accessExpiration;
	}

	public void setAccessExpiration(String accessExpiration) {
		this.accessExpiration = accessExpiration;
	}

	public String getEmbeddedToken() {
		return this.embeddedToken;
	}

	public void setEmbeddedToken(String embeddedToken) {
		this.embeddedToken = embeddedToken;
	}

	public String getEmbeddedExpiration() {
		return this.embeddedExpiration;
	}

	public void setEmbeddedExpiration(String embeddedExpiration) {
		this.embeddedExpiration = embeddedExpiration;
	}

	@Override
	public String toString() {
		return "PowerBiInformation{" +
				"airline='" + this.airline + '\'' +
				", workspaceId='" + this.workspaceId + '\'' +
				", reportId='" + this.reportId + '\'' +
				", accessToken='" + this.accessToken + '\'' +
				", accessExpiration='" + this.accessExpiration + '\'' +
				", embeddedToken='" + this.embeddedToken + '\'' +
				", embeddedExpiration='" + this.embeddedExpiration + '\'' +
				'}';
	}
}
