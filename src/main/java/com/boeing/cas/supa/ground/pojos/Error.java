package com.boeing.cas.supa.ground.pojos;

public class Error {

	protected String errorLabel;
	protected String errorDescription;
	protected long timestamp;

	public Error() {}

	public Error(String error, String errorDescription) {

		this.errorLabel = error;
		this.errorDescription = errorDescription;
		this.timestamp = System.currentTimeMillis() / 1_000L;
	}

	public String getErrorLabel() {
		return this.errorLabel;
	}

	public void setErrorLabel(String errorLabel) {
		this.errorLabel = errorLabel;
	}

	public String getErrorDescription() {
		return this.errorDescription;
	}

	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
