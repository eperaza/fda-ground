package com.boeing.cas.supa.ground.pojos;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "ActiveTSP")
public class ActiveTsp extends BaseEntity {
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "TspID", nullable = false)
	@JsonIgnore
	private Tsp tsp;
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "AircraftInfoID", nullable = false)
	@JsonIgnore
	private AircraftInfo aircraftInfo;

	public Tsp getTsp() {
		return tsp;
	}

	public void setTsp(Tsp tsp) {
		this.tsp = tsp;
	}

	public AircraftInfo getAircraftInfo() {
		return aircraftInfo;
	}

	public void setAircraftInfo(AircraftInfo aircraftInfo) {
		this.aircraftInfo = aircraftInfo;
	}
}