package com.boeing.cas.supa.ground.dao;

import java.util.List;

import com.boeing.cas.supa.ground.exceptions.FlightRecordException;
import com.boeing.cas.supa.ground.pojos.FlightCount;
import com.boeing.cas.supa.ground.pojos.FlightRecord;

public interface FlightRecordDao {

	public void insertFlightData(FlightRecord flightRecord) throws FlightRecordException;

	public List<FlightRecord> getAllFlightRecords(String airline) throws FlightRecordException;

	public String getLatestSupaVersion(String airline, String airlineTail);

	public List<FlightCount> getAllFlightCounts(String airline) throws FlightRecordException;

	public FlightRecord getFlightRecord(String flightRecordName) throws FlightRecordException;

	public void updateFlightRecordOnAidStatus(String flightRecordName) throws FlightRecordException;
}
