package com.boeing.cas.supa.ground.pojos;

public class FlightCount {

	private String tail;
	private int count = 0;
	private int processed = 0;

	public FlightCount(String tail) {
		this.tail = tail;
	}

	public FlightCount(String tail, int count) {

		this.tail = tail;
		this.count = count;
	}

	public FlightCount(String tail, int count, int processed) {

		this.tail = tail;
		this.count = count;
		this.processed = processed;
	}

	public String getTail() {
		return tail;
	}

	public void setTail(String tail) {
		this.tail = tail;
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

