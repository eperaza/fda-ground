package com.boeing.cas.supa.ground.dao;

import java.util.List;
import com.boeing.cas.supa.ground.pojos.Airline;

public interface AirlineDao {
	public boolean save(Airline airline);
	public List<Airline> getAirlines();
	public Airline getAirlineById(int id);
	public Airline getAirlineByName(String name);
}