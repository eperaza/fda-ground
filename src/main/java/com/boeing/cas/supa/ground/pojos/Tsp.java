package com.boeing.cas.supa.ground.pojos;

import java.sql.Timestamp;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
	@NamedQuery(name = "getTspListByAirline", query = "SELECT tsp FROM Tsp tsp WHERE tsp.airlineTail.airline.name = :airlineName ORDER BY tsp.airlineTail.tailNumber ASC, tsp.version DESC"),
	@NamedQuery(name = "getTspListByAirlineAndTailNumber", query = "SELECT tsp FROM Tsp tsp WHERE tsp.airlineTail.airline.name = :airlineName AND tsp.airlineTail.tailNumber = :tailNumber"),
	@NamedQuery(name = "getTspListByAirlineAndTailNumberAndStage", query = "SELECT tsp FROM Tsp tsp "
			+ "WHERE tsp.airlineTail.airline.name = :airlineName AND tsp.airlineTail.tailNumber = :tailNumber AND tsp.stage = :stage"),
	@NamedQuery(name = "getActiveTspByAirlineAndTailNumberAndStage", query = "SELECT tsp FROM Tsp tsp "
			+ "WHERE tsp.airlineTail.airline.name = :airlineName AND tsp.airlineTail.tailNumber = :tailNumber AND tsp.stage = :stage AND tsp.activeTsp != null"),
	@NamedQuery(name = "getTspByAirlineAndTailNumberAndVersionAndStage", query = "SELECT tsp FROM Tsp tsp "
			+ "WHERE tsp.airlineTail.airline.name = :airlineName AND tsp.airlineTail.tailNumber = :tailNumber AND tsp.version = :version "
			+ "AND tsp.stage = :stage")
})
public class Tsp extends BaseEntity {
	public enum Stage {
		PROD,
		DEV,
		TEST
	};
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "AirlineTailId", nullable = false)
	private AirlineTail airlineTail;
	
	private String tspContent;
	private String version;
	
	@OneToOne(mappedBy = "tsp", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
	private ActiveTsp activeTsp;
	
	private Timestamp effectiveDate;
	
	@Enumerated(EnumType.STRING)
	private Stage stage;
	
	public AirlineTail getAirlineTail() {
		return airlineTail;
	}
	
	public void setAirlineTail(AirlineTail airlineTail) {
		this.airlineTail = airlineTail;
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
	
	public Timestamp getEffectiveDate() {
		return effectiveDate;
	}
	
	public void setEffectiveDate(Timestamp effectiveDate) {
		this.effectiveDate = effectiveDate;
	}
	
	public Stage getStage() {
		if (stage == null) {
			stage = Stage.PROD;
		}
		return stage;
	}
	
	public void setStage(Stage stage) {
		this.stage = stage;
	}
}
