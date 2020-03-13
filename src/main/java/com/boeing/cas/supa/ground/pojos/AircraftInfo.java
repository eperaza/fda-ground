package com.boeing.cas.supa.ground.pojos;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "AircraftInfo")
@NamedQuery(name = "getTailByAirlineAndTailNumber", query = "SELECT al FROM AircraftInfo al WHERE al.airline.name = :name AND al.tailNumber = :tailNumber")
public class AircraftInfo extends BaseEntity {
	private String tailNumber;
	
	@Column(name = "Active")
	private boolean isActive = true;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "AirlineId", nullable = false)
	private Airline airline;
	
	@OneToMany(mappedBy = "aircraftInfo", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
	@JsonIgnore
	private Set<Tsp> tsps;
	
	@OneToOne(mappedBy = "aircraftInfo", fetch = FetchType.LAZY)
	private ActiveTsp activeTsp;
	
	public String getTailNumber() {
		return tailNumber;
	}
	
	public void setTailNumber(String tailNumber) {
		this.tailNumber = tailNumber;
	}
	
	public boolean getIsActive() {
		return isActive;
	}
	
	public void setIsActive(boolean isActive) {
		this.isActive = isActive;
	}
	
	public Airline getAirline() {
		return airline;
	}
	
	public void setAirline(Airline airline) {
		this.airline = airline;
	}

	public Set<Tsp> getTsps() {
		if (tsps == null) {
			tsps = new HashSet<Tsp>();
		}
		return tsps;
	}

	public void setTsps(Set<Tsp> tsps) {
		this.tsps = tsps;
	}
	
	public ActiveTsp getActiveTsp() {
		return this.activeTsp;
	}
	
	public void setActiveTsp(ActiveTsp activeTsp) {
		this.activeTsp = activeTsp;
	}
	
	@JsonIgnore
	public Tsp getCurrentActiveTsp() {
		return this.activeTsp == null ? null : this.activeTsp.getTsp();
	}
}
