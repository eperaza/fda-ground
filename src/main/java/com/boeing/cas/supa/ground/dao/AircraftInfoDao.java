package com.boeing.cas.supa.ground.dao;

import com.boeing.cas.supa.ground.pojos.AircraftConfiguration;
import com.boeing.cas.supa.ground.pojos.AircraftInfo;

import java.util.Date;
import java.util.List;

public interface AircraftInfoDao {
	public boolean save(AircraftInfo airlineTail);
	public List<AircraftInfo> getAirlineTails();
	public AircraftInfo getAirlineTailById(int id);
	public Date getAircraftPropertyLastModifiedTimeStamp(String airlineName, String tailNumber);
	public AircraftInfo getTailByAirlineAndTailNumber(String airlineName, String tailNumber);
	public AircraftConfiguration getAircarftPropertiesByAirlineAndTailNumber(String airlineName, String tailNumber);
}