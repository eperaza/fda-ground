package com.boeing.cas.supa.ground.pojos;

public class SupaRelease {

	private String release;
	private String partNumber;
	private String path;
	private String airline;
	private byte[] file;
	
	public SupaRelease(String release, String partNumber, String path) {
		this(release, partNumber, path, null);
	}

	public SupaRelease(String release, String partNumber, String path, String airline) {
		this.release = release;
		this.partNumber = partNumber;
		this.path = path;
		this.airline = airline;
	}

	public String getRelease() {
		return release;
	}

	public void setRelease(String release) {
		this.release = release;
	}

	public String getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getAirline() {
		return airline;
	}

	public void setAirline(String airline) {
		this.airline = airline;
	}

	public byte[] getFile() {
		return file;
	}

	public void setFile(byte[] file) {
		this.file = file;
	}
}
