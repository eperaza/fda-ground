package com.boeing.cas.supa.ground.pojos;

public class Error {
	protected String error;
	protected String errorDescription;
	protected long timestamp;
	
	public Error() {

	}
	/**
	 * @param error
	 * @param errorDescription
	 * @param timestamp
	 */
	public Error(String error, String errorDescription) {
		this.error = error;
		this.errorDescription = errorDescription;
		this.timestamp = System.currentTimeMillis()/1000;
	}
	/**
	 * @return the error
	 */
	public String getError() {
		return error;
	}
	/**
	 * @param error the error to set
	 */
	public void setError(String error) {
		this.error = error;
	}
	/**
	 * @return the errorDescription
	 */
	public String getErrorDescription() {
		return errorDescription;
	}
	/**
	 * @param errorDescription the errorDescription to set
	 */
	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}
	/**
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}
	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	

}
