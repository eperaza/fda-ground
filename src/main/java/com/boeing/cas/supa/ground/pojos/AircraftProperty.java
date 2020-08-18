package com.boeing.cas.supa.ground.pojos;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "AircraftProperty")
public class AircraftProperty extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "AircraftInfoID", nullable = false)
	private AircraftInfo aircraftInfo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PropertyValueID", nullable = false)
	private PropertyValue propertyValue;

	public AircraftInfo getAircraftInfo() {
		return aircraftInfo;
	}

	public void setAircraftInfo(AircraftInfo aircraftInfo) {
		this.aircraftInfo = aircraftInfo;
	}

	public PropertyValue getPropertyValue() {
		return propertyValue;
	}

	public void setPropertyValue(PropertyValue propertyValue) {
		this.propertyValue = propertyValue;
	}
}