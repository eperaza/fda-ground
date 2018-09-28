package com.boeing.cas.supa.ground.pojos;

public class PlaybackDemoFlight {

	private String flightStreamName;
	private String path;
	private byte[] file;

	public PlaybackDemoFlight(String flightStreamName, String path) {
		this.flightStreamName = flightStreamName;
		this.path = path;
	}

	public String getFlightStreamName() {
		return flightStreamName;
	}

	public void setFlightStreamName(String flightStreamName) {
		this.flightStreamName = flightStreamName;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public byte[] getFile() {
		return file;
	}

	public void setFile(byte[] file) {
		this.file = file;
	}
}
