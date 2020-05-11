package com.boeing.cas.supa.ground.dao;

import java.util.List;

import com.boeing.cas.supa.ground.pojos.AircraftConfiguration;
import com.boeing.cas.supa.ground.pojos.AircraftInfo;

public interface AircraftInfoDao {
	public boolean save(AircraftInfo airlineTail);
	public List<AircraftInfo> getAirlineTails();
	public AircraftInfo getAirlineTailById(int id);
	public AircraftInfo getTailByAirlineAndTailNumber(String airlineName, String tailNumber);
	public AircraftConfiguration getAircarftPropertiesByAirlineAndTailNumber(String airlineName, String tailNumber);
}