package com.boeing.cas.supa.ground.dao;

import java.util.Date;
import java.util.List;

import com.boeing.cas.supa.ground.pojos.Tsp;

public interface TspDao {
	public boolean save(Tsp tsp);
	public List<Tsp> getAllTsps();
	public List<Tsp> getTspListByAirline(String airlineName);
	public List<Tsp> getTspListByAirlineAndTailNumber(String airlineName, String tailNumber);
	public Tsp getTspById(int id);
	public Tsp getActiveTspByAirlineAndTailNumberAndStage(String airlineName, String tailNumber, Tsp.Stage stage);
	public Tsp getTspByAirlineAndTailNumberAndModifiedDateAndEffectiveDateAndStage(
			String airlineName, String tailNumber, Date modifiedDate, Date effectiveDate, Tsp.Stage stage);
	public Tsp getTspByAirlineAndTailNumberAndVersionAndStage(String airlineName, String tailNumber, String version, Tsp.Stage stage);
}