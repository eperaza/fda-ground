package com.boeing.cas.supa.ground.pojos;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "AircraftInfo")
@NamedQueries({
	@NamedQuery(name = "getTailByAirlineAndTailNumber", query = "SELECT al FROM AircraftInfo al WHERE al.airline.name = :name AND al.tailNumber = :tailNumber"),
	@NamedQuery(name = "getAircarftPropertiesByAirlineAndTailNumber", query = "SELECT new com.boeing.cas.supa.ground.pojos.AircraftConfiguration(a) "
			+ "FROM AircraftInfo a WHERE a.airline.name = :name AND a.tailNumber = :tailNumber ")
})
public class AircraftInfo extends BaseEntity {
	private String tailNumber;
	
	@Column(name = "Active")
	private boolean isActive = true;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "AirlineId", nullable = false)
	private Airline airline;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "AircraftTypeID", nullable = false)
	private AircraftType aircraftType;
	
	@OneToMany(mappedBy = "aircraftInfo", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
	@JsonIgnore
	private Set<AircraftProperty> aircraftProperties;
	
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

	public AircraftType getAircraftType() {
		return aircraftType;
	}

	public void setAircraftType(AircraftType aircraftType) {
		this.aircraftType = aircraftType;
	}

	public Set<AircraftProperty> getAircraftProperties() {
		if (this.aircraftProperties == null) {
			this.aircraftProperties = new HashSet<AircraftProperty>();
		}
		return aircraftProperties;
	}

	public void setAircraftProperties(Set<AircraftProperty> aircraftProperties) {
		this.aircraftProperties = aircraftProperties;
	}
	
	@JsonIgnore
	public Tsp getCurrentActiveTsp() {
		return this.activeTsp == null ? null : this.activeTsp.getTsp();
	}
}
