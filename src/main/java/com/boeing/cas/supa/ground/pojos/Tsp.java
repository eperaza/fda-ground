package com.boeing.cas.supa.ground.pojos;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "TSP")
@NamedQueries({
	@NamedQuery(name = "getTspListByAirline", query = "SELECT tsp FROM Tsp tsp WHERE tsp.aircraftInfo.airline.name = :airlineName ORDER BY tsp.aircraftInfo.tailNumber ASC, tsp.version DESC"),
	@NamedQuery(name = "getTspListByAirlineAndTailNumber", query = "SELECT tsp FROM Tsp tsp WHERE tsp.aircraftInfo.airline.name = :airlineName AND tsp.aircraftInfo.tailNumber = :tailNumber"),
	@NamedQuery(name = "getActiveTspByAirlineAndTailNumber", query = "SELECT tsp FROM Tsp tsp "
			+ "WHERE tsp.aircraftInfo.airline.name = :airlineName AND tsp.aircraftInfo.tailNumber = :tailNumber AND tsp.activeTsp.id is not null"),
	@NamedQuery(name = "getTspByAirlineAndTailNumberAndVersion", query = "SELECT tsp FROM Tsp tsp "
			+ "WHERE tsp.aircraftInfo.airline.name = :airlineName AND tsp.aircraftInfo.tailNumber = :tailNumber AND tsp.version = :version")
})
public class Tsp extends BaseEntity {
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "AircraftInfoID", nullable = false)
	private AircraftInfo aircraftInfo;
	
	private String tspContent;
	private String version;
	
	@OneToOne(mappedBy = "tsp", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
	private ActiveTsp activeTsp;
	
	public AircraftInfo getAircraftInfo() {
		return aircraftInfo;
	}
	
	public void setAircraftInfo(AircraftInfo aircraftInfo) {
		this.aircraftInfo = aircraftInfo;
	}
	
	public String getTspContent() {
		return tspContent;
	}
	
	public void setTspContent(String tspContent) {
		this.tspContent = tspContent;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public ActiveTsp getActiveTsp() {
		return this.activeTsp;
	}
	
	public void setActiveTsp(ActiveTsp activeTsp) {
		this.activeTsp = activeTsp;
	}
}
