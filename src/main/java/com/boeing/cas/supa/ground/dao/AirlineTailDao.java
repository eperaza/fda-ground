package com.boeing.cas.supa.ground.dao;

import java.util.List;

import com.boeing.cas.supa.ground.pojos.AirlineTail;

public interface AirlineTailDao {
	public boolean save(AirlineTail airlineTail);
	public List<AirlineTail> getAirlineTails();
	public AirlineTail getAirlineTailById(int id);
	public AirlineTail getTailByAirlineAndTailNumber(String airlineName, String tailNumber);
}