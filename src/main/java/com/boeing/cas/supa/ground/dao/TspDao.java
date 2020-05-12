package com.boeing.cas.supa.ground.dao;

import java.util.List;

import com.boeing.cas.supa.ground.pojos.Tsp;

public interface TspDao {
	public boolean save(Tsp tsp);
	public List<Tsp> getTsps();
	public List<Tsp> getTsps(String airlineName);
	public List<Tsp> getTsps(String airlineName, String tailNumber);
	public Tsp getTspById(int id);
	public Tsp getActiveTspByAirlineAndTailNumber(String airlineName, String tailNumber);
	public Tsp getTspByAirlineAndTailNumberAndVersion(String airlineName, String tailNumber, String version);
}