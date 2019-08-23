package com.boeing.cas.supa.ground.pojos;

public class FlightCount {

	private String tail;
	private int count = 0;
	private int processed = 0;
	private String version = new String("unknown");

	public FlightCount(String tail) {
		this.tail = tail;
	}

	public FlightCount(String tail, int count) {

		this.tail = tail;
		this.count = count;
	}

	public FlightCount(String tail, int count, int processed, String version) {

		this.tail = tail;
		this.count = count;
		this.processed = processed;
		this.version = version;
	}

	public String getTail() {
		return tail;
	}

	public void setTail(String tail) {
		this.tail = tail;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getProcessed() {
		return processed;
	}

	public void setProcessed(int processed) {
		this.processed = processed;
	}

}

