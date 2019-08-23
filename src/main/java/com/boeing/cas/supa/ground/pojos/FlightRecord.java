package com.boeing.cas.supa.ground.pojos;

import com.boeing.cas.supa.ground.services.FileManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class FlightRecord implements Comparable<FlightRecord> {

	private final Logger logger = LoggerFactory.getLogger(FlightRecord.class);

	private String flightRecordName;
	private String storagePath;
	private long fileSizeKb;
	private Instant flightDatetime;
	private String aidId;
	private String airline;
	private String userId;
	private String status;
	private boolean uploadToAdw;
	private boolean deletedOnAid;
	private boolean processedByAnalytics;

	public FlightRecord(String flightRecordName, String storagePath, long fileSizeKb, Instant flightDatetime,
			String aidId, String airline, String userId, String status, boolean uploadToAdw, boolean deletedOnAid,
			boolean processedByAnalytics) {

		this.flightRecordName = flightRecordName;
		this.storagePath = storagePath;
		this.fileSizeKb = fileSizeKb;
		this.flightDatetime = flightDatetime;
		this.aidId = aidId;
		this.airline = airline;
		this.userId = userId;
		this.status = status;
		this.uploadToAdw = uploadToAdw;
		this.deletedOnAid = deletedOnAid;
		this.processedByAnalytics = processedByAnalytics;
	}

	public String getFlightRecordName() {
		return flightRecordName;
	}

	public void setFlightRecordName(String flightRecordName) {
		this.flightRecordName = flightRecordName;
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

	public Instant getFlightDatetime() {
		return flightDatetime;
	}

	public void setFlightDatetime(Instant flightDatetime) {
		this.flightDatetime = flightDatetime;
	}

	public String getAidId() {
		return aidId;
	}

	public void setAidId(String aidId) {
		this.aidId = aidId;
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

	public boolean isUploadToAdw() {
		return uploadToAdw;
	}

	public void setUploadToAdw(boolean uploadToAdw) {
		this.uploadToAdw = uploadToAdw;
	}

	public boolean isDeletedOnAid() {
		return deletedOnAid;
	}

	public void setDeletedOnAid(boolean deletedOnAid) {
		this.deletedOnAid = deletedOnAid;
	}

	public boolean isProcessedByAnalytics() {
		return processedByAnalytics;
	}

	public void setProcessedByAnalytics(boolean processedByAnalytics) {
		this.processedByAnalytics = processedByAnalytics;
	}

	@Override
	public String toString() {

		return new StringBuilder('[').append(this.getClass().getSimpleName()).append(']').append(':')
				.append("flightRecordName=").append(this.flightRecordName).append(',')
				.append("storagePath=").append(this.storagePath).append(',')
				.append("fileSizeKb=").append(this.fileSizeKb).append(',')
				.append("flightDatetime=").append(this.flightDatetime).append(',')
				.append("aidId=").append(this.aidId).append(',')
				.append("airline=").append(this.airline).append(',')
				.append("userId=").append(this.userId).append(',')
				.append("status=").append(this.status).append(',')
				.append("uploadToAdw=").append(this.uploadToAdw).append(',')
				.append("deletedOnAid=").append(this.deletedOnAid).append(',')
				.append("processedByAnalytics=").append(this.processedByAnalytics)
			.toString();
	}


	@Override
	public int compareTo(FlightRecord that) {
		// just use airline code and tail number (XXX/XXXXXX/..)
		// *note index could return -1; if so just use getStorage

		if (this.getStoragePath().indexOf('/', 5) < 0) {
			return this.storagePath.compareTo(that.storagePath);
		}
		if (that.getStoragePath().indexOf('/', 5) < 0) {
			return this.storagePath.compareTo(that.storagePath);
		}

		// Sort by airline code and tail; then by flight_datetime
		// *note return latest date first!
		if (this.getStoragePath().substring(0,this.getStoragePath().indexOf('/',5))
			.equalsIgnoreCase(that.getStoragePath().substring(0, that.getStoragePath().indexOf('/', 5))))
		{
			return that.getFlightDatetime().compareTo(this.flightDatetime);
		} else {
			return this.storagePath.compareTo(that.storagePath);
		}

	}
}
