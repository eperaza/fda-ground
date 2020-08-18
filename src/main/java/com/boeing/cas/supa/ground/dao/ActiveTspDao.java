package com.boeing.cas.supa.ground.dao;

import com.boeing.cas.supa.ground.pojos.ActiveTsp;
import com.boeing.cas.supa.ground.pojos.Tsp;

public interface ActiveTspDao {
	public boolean activateTsp(Tsp tsp);
	public void deleteActiveTsp(ActiveTsp activeTsp);
}