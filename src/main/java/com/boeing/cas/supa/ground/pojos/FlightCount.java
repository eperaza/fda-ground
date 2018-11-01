package com.boeing.cas.supa.ground.pojos;

public class FlightCount {

	private String tail;
	private int count = 0;

	public FlightCount(String tail) {
		this.tail = tail;
	}

	public FlightCount(String tail, int count) {

		this.tail = tail;
		this.count = count;
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
}
