package com.boeing.cas.supa.ground.dao;

import java.util.List;
import com.boeing.cas.supa.ground.pojos.AircraftType;

public interface AircraftTypeDao {
	public List<AircraftType> getAircraftTypes();
	public AircraftType getAircraftTypeById(int id);
	public AircraftType getAircraftTypeByName(String name);
}