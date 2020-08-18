package com.boeing.cas.supa.ground.pojos;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "PropertyValue")
public class PropertyValue extends BaseEntity {
	private String value;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PlatformPropertyID", nullable = false)
	private PlatformProperty platformProperty;
	
	@OneToMany(mappedBy = "propertyValue", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
	@JsonIgnore
	private Set<AircraftProperty> aircraftProperties;
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}

	public PlatformProperty getPlatformProperty() {
		return platformProperty;
	}

	public void setPlatformProperty(PlatformProperty platformProperty) {
		this.platformProperty = platformProperty;
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
}