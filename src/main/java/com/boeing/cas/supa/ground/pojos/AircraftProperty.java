package com.boeing.cas.supa.ground.pojos;

import javax.persistence.*;

@Entity
@Table(name = "AircraftProperty")
@NamedQueries({
	@NamedQuery(name="getAircraftPropertyLastModifiedTimeStamp", query = "SELECT CASE " +
			"WHEN MAX(ap.modifiedTime) > MAX(ai.modifiedTime) " +
			"THEN  MAX(ap.modifiedTime) ELSE MAX(ai.modifiedTime) " +
			"END as MostRecentTime " +
			"FROM AircraftProperty ap,	AircraftInfo ai, Airline a " +
			"WHERE ap.aircraftInfo.airline.name = :name"	)
})
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