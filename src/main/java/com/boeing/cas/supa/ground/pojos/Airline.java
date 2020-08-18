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
@Table(name = "Airline")
@NamedQuery(name = "getAirlineByName", query = "SELECT a FROM Airline a WHERE a.name = :name")
public class Airline extends BaseEntity {
	private String name;
	
	@OneToMany(mappedBy = "airline", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
	@JsonIgnore
	private Set<AircraftInfo> tails;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public Set<AircraftInfo> getTails() {
		if (tails == null) {
			tails = new HashSet<AircraftInfo>();
		}
		return tails;
	}

	public void setTails(Set<AircraftInfo> tails) {
		this.tails = tails;
	}
}