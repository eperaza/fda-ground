package com.boeing.cas.supa.ground.pojos;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "AircraftType")
@NamedQuery(name = "getAircraftTypeByName", query = "select a from AircraftType a where a.name = :name")
public class AircraftType extends BaseEntity {
	private String name;
	
	@OneToMany(mappedBy = "aircraftType", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
	@JsonIgnore
	private Set<AircraftInfo> aircrafts;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public Set<AircraftInfo> getAircrafts() {
		if (aircrafts == null) {
			aircrafts = new HashSet<AircraftInfo>();
		}
		return aircrafts;
	}

	public void setAircrafts(Set<AircraftInfo> aircrafts) {
		this.aircrafts = aircrafts;
	}
}