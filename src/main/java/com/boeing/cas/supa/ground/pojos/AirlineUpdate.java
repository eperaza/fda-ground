package com.boeing.cas.supa.ground.pojos;

public class AirlineUpdate {
	private String airline;
	private String updated;
	

	public AirlineUpdate(String airline, String updated) {
		this.airline = airline;
		this.updated = updated;
	}

	public String getAirline() {
		return airline;
	}

	public void setAirline(String airline) {
		this.airline = airline;
	}

	public String getUpdated() {
		return updated;
	}

	public void setPath(String updated) {
		this.updated = updated;
	}

}
