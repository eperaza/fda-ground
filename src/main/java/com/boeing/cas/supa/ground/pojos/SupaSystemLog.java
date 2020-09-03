package com.boeing.cas.supa.ground.pojos;

import java.time.Instant;

public class SupaSystemLog {

	private String supaSystemLogName;
	private String storagePath;
	private long fileSizeKb;
	private Instant logDatetime;
	private String airline;
	private String userId;
	private String status;

	public SupaSystemLog(String supaSystemLogName, String storagePath, long fileSizeKb, Instant logDatetime,
			String airline, String userId, String status) {

		this.supaSystemLogName = supaSystemLogName;
		this.storagePath = storagePath;
		this.fileSizeKb = fileSizeKb;
		this.logDatetime = logDatetime;
		this.airline = airline;
		this.userId = userId;
		this.status = status;
	}

	public String getSupaSystemLogName() {
		return supaSystemLogName;
	}

	public void setSupaSystemLogName(String supaSystemLogName) {
		this.supaSystemLogName = supaSystemLogName;
	}

	public String getStoragePath() {
		return storagePath;
	}

	public void setStoragePath(String storagePath) {
		this.storagePath = storagePath;
	}

	public long getFileSizeKb() {
		return fileSizeKb;
	}

	public void setFileSizeKb(long fileSizeKb) {
		this.fileSizeKb = fileSizeKb;
	}

	public Instant getLogDatetime() {
		return logDatetime;
	}

	public void setLogDatetime(Instant logDatetime) {
		this.logDatetime = logDatetime;
	}

	public String getAirline() {
		return airline;
	}

	public void setAirline(String airline) {
		this.airline = airline;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {

		return new StringBuilder("[").append(this.getClass().getSimpleName()).append(']').append(':')
				.append("supaSystemLogName=").append(this.supaSystemLogName).append(',')
				.append("storagePath=").append(this.storagePath).append(',')
				.append("fileSizeKb=").append(this.fileSizeKb).append(',')
				.append("logDatetime=").append(this.logDatetime).append(',')
				.append("airline=").append(this.airline).append(',')
				.append("userId=").append(this.userId).append(',')
				.append("status=").append(this.status).append(',')
			.toString();
	}
}
