package com.boeing.cas.supa.ground.pojos;

import java.sql.Date;

public class AirlineUpdate {
	private String airline;
	private java.util.Date updated;
	

	public AirlineUpdate(String airline, java.util.Date updatedDate) {
		this.airline = airline;
		this.updated = updatedDate;
	}

	public String getAirline() {
		return airline;
	}

	public void setAirline(String airline) {
		this.airline = airline;
	}

	public java.util.Date getUpdated() {
		return updated;
	}

	public void setUpdateDate(java.util.Date dt1) {
		this.updated = dt1;
	}

}
