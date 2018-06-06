package com.boeing.cas.supa.ground.pojos;

public class UploadFlightRecordMessage {

	private boolean uploaded;
	private String flightRecordName;
	private boolean deletedOnAid;
	private String message;

	public UploadFlightRecordMessage(String flightRecordName) {
		this.flightRecordName = flightRecordName;
	}

	public UploadFlightRecordMessage(boolean uploaded, String flightRecordName, boolean deletedOnAid, String message) {

		this.uploaded = uploaded;
		this.flightRecordName = flightRecordName;
		this.deletedOnAid = deletedOnAid;
		this.message = message;
	}

	public boolean isUploaded() {
		return this.uploaded;
	}

	public void setUploaded(boolean uploaded) {
		this.uploaded = uploaded;
	}

	public String getFlightRecordName() {
		return flightRecordName;
	}

	public void setFlightRecordName(String flightRecordName) {
		this.flightRecordName = flightRecordName;
	}

	public boolean isDeletedOnAid() {
		return deletedOnAid;
	}

	public void setDeletedOnAid(boolean deletedOnAid) {
		this.deletedOnAid = deletedOnAid;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
