package com.boeing.cas.supa.ground.pojos;

public class CurrentSupaRelease {

	private String release;
	private String description;
	private String releaseDate;
	private String airline;
	private String updatedBy;


	public CurrentSupaRelease(String release, String description, String updatedBy, String airline) {
		this.release = release;
		this.description = description;
		this.updatedBy = updatedBy;
		this.airline = airline;
	}

	public CurrentSupaRelease(String release, String description, String releaseDate, String updatedBy, String airline) {
		this.release = release;
		this.description = description;
		this.releaseDate = releaseDate;
		this.updatedBy = updatedBy;
		this.airline = airline;
	}

	public String getRelease() {
		return release;
	}

	public void setRelease(String release) {
		this.release = release;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(String releaseDate) {
		this.releaseDate = releaseDate;
	}

	public String getAirline() {
		return airline;
	}

	public void setAirline(String airline) {
		this.airline = airline;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}
}
