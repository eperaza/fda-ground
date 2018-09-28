package com.boeing.cas.supa.ground.dao;

import java.util.List;

import com.boeing.cas.supa.ground.exceptions.PlaybackDemoFlightException;
import com.boeing.cas.supa.ground.pojos.PlaybackDemoFlight;

public interface PlaybackDemoFlightMgmtDao {

	public List<PlaybackDemoFlight> listDemoFlightStreams() throws PlaybackDemoFlightException;
	
	public PlaybackDemoFlight getDemoFlightStream(String flightStreamName) throws PlaybackDemoFlightException;
}
