package com.boeing.cas.supa.ground.pojos;

import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

public class ApiError {

	private String errorLabel;
	private String errorDescription;
	private RequestFailureReason failureReason;
	private long timestamp;

	public ApiError() {}

	public ApiError(String error, String errorDescription) {

		this.errorLabel = error;
		this.errorDescription = errorDescription;
		this.failureReason = RequestFailureReason.INTERNAL_SERVER_ERROR;
		this.timestamp = System.currentTimeMillis() / 1_000L;
	}

	public ApiError(String error, String errorDescription, RequestFailureReason failureReason) {

		this.errorLabel = error;
		this.errorDescription = errorDescription;
		this.failureReason = failureReason;
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

	public RequestFailureReason getFailureReason() {
		return failureReason;
	}

	public void setFailureReason(RequestFailureReason failureReason) {
		this.failureReason = failureReason;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
