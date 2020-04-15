package com.boeing.cas.supa.ground.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.boeing.cas.supa.ground.pojos.FlightCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.boeing.cas.supa.ground.exceptions.FlightRecordException;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.FlightRecord;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

import microsoft.sql.DateTimeOffset;

@Repository
public class FlightRecordDaoImpl implements FlightRecordDao {

	private final Logger logger = LoggerFactory.getLogger(FlightRecordDaoImpl.class);

	private static final String GET_FLIGHT_RECORD = "SELECT * FROM flight_records WHERE flight_record_name = :flight_record_name";
	private static final String GET_ALL_FLIGHT_RECORDS = "SELECT * FROM flight_records WHERE airline = :airline ORDER BY storage_path";
	private static final String INSERT_FLIGHT_RECORD = "INSERT INTO flight_records (flight_record_name, storage_path, file_size_kb, flight_datetime, aid_id, airline, user_id, upload_to_adw) VALUES (:flight_record_name, :storage_path, :file_size_kb, :flight_datetime, :aid_id, :airline, :user_id, :upload_to_adw)";
	private static final String UPDATE_FLIGHT_RECORD_DELETED_ON_AID = "UPDATE flight_records SET deleted_on_aid = 1 WHERE flight_record_name = :flight_record_name AND deleted_on_aid = 0";

	// used to obtain flight/analytic counts and current supa version for each valid tail (UNRESOLVED taill will be filtered out)
	// first obtain tail from storage_path: AMX/eidra/201810 or AMX using: substring(left (storage_path, CHARINDEX('/', storage_path, 7)), 5, 25)
	// then resolve counts using COUNT and SUM
	// finally obtain the supa version, using the aid_id associated with the most recent (MAX) flight_datetime
	private static final String GET_FLIGHT_COUNTS = "SELECT tail AS tailNumber,"
		+ " COUNT(airline) AS uploaded_by_fda, SUM(analytics) AS processed_by_analytics,"
		+ " SUBSTRING(MAX(supa),36,50) AS supa_version FROM"
		+ " (SELECT airline, CONCAT(flight_datetime,'~',aid_id) AS supa,"
		+ " SUBSTRING(LEFT (storage_path, CHARINDEX('/', storage_path, 7)), 5, 20) AS tail,"
		+ " (CASE WHEN processed_by_analytics=1 THEN 1 ELSE 0 END) AS analytics"
		+ " FROM flight_records WHERE airline = :airline) info WHERE tail != '' GROUP BY tail ORDER BY tailNumber";

	private static final String GET_LATEST_EXISTING_SUPA_VERSION = "SELECT SUBSTRING(MAX(supa),36,50) AS supa_version"
		+ " FROM (SELECT CONCAT(flight_datetime,'~',aid_id) AS supa,"
		+ " (LEFT (storage_path, CHARINDEX('/', storage_path, 7))) AS airlinetail"
		+ " FROM flight_records WHERE aid_id != 'pending' AND airline = :airline ) info"
		+ " WHERE airlinetail = :airline_tail GROUP BY airlinetail";

	@Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

	@Override
	public void insertFlightData(FlightRecord flightRecord) throws FlightRecordException {

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("flight_record_name", flightRecord.getFlightRecordName());
		namedParameters.put("storage_path", flightRecord.getStoragePath());
		namedParameters.put("file_size_kb", flightRecord.getFileSizeKb());
		namedParameters.put("flight_datetime", flightRecord.getFlightDatetime());
		namedParameters.put("flight_datetime", DateTimeOffset.valueOf(Timestamp.from(flightRecord.getFlightDatetime()), 0));
		namedParameters.put("aid_id", flightRecord.getAidId());
		namedParameters.put("airline", flightRecord.getAirline());
		namedParameters.put("user_id", flightRecord.getUserId());
		namedParameters.put("upload_to_adw", flightRecord.isUploadToAdw());
		try {

			int returnVal = jdbcTemplate.update(INSERT_FLIGHT_RECORD, namedParameters);
			if (returnVal != 1) {
				logger.error("Could not insert flight record into database: {} record(s) updated", returnVal);
				throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPDATE_STATUS_FAILURE", String.format("%d record(s) updated", returnVal), RequestFailureReason.INTERNAL_SERVER_ERROR));
			}
		} catch (DataAccessException dae) {
			logger.error("Failed to insert flight data into database: {}", dae.getMessage(), dae);
			throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPDATE_STATUS_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}

	@Override
	public List<FlightRecord> getAllFlightRecords(String airline) throws FlightRecordException {

		List<FlightRecord> flightRecords = new ArrayList<>();
		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("airline", airline);

		try {

			flightRecords = jdbcTemplate.query(GET_ALL_FLIGHT_RECORDS, namedParameters, new FlightRecordRowMapper());
		} catch (DataAccessException dae) {
			logger.error("Failed to getAllFlightRecords: {}", dae.getMessage(), dae);
			throw new FlightRecordException(new ApiError("FLIGHT_RECORD_RETRIEVAL_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

		return flightRecords;
	}


	@Override
	public String getLatestSupaVersion(String airline, String airlineTail) {

		String supaVersion = "unknown";
		List<FlightRecord> flightRecords = new ArrayList<>();
		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("airline", airline);
		namedParameters.put("airline_tail", airlineTail);

		try {
			List<String> versions = jdbcTemplate.query(GET_LATEST_EXISTING_SUPA_VERSION, namedParameters, new SupaVersionRowMapper());

			logger.info("obtained Supa Version: {}", versions.size());

			supaVersion = versions.size() > 0 ? versions.get(0) : "pending";

		} catch (EmptyResultDataAccessException edb) {
			logger.error("Failed to getLatestSupaVersion: <not found> {}", edb.getMessage());
		} catch (DataAccessException dae) {
			logger.error("Failed to getLatestSupaVersion: {}", dae.getMessage(), dae);
			//throw new FlightRecordException(new ApiError("SUPA_VERSION_RETRIEVAL_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
		return supaVersion;
	}

	@Override
	public List<FlightCount> getAllFlightCounts(String airline) throws FlightRecordException {

		List<FlightCount> flightCounts = new ArrayList<>();
		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("airline", airline);

		try {

			flightCounts = jdbcTemplate.query(GET_FLIGHT_COUNTS, namedParameters, new FlightCountRowMapper());
		} catch (DataAccessException dae) {
			logger.error("Failed to getAllFlightCounts: {}", dae.getMessage(), dae);
			throw new FlightRecordException(new ApiError("FLIGHT_RECORD_RETRIEVAL_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

		return flightCounts;
	}

	@Override
	public FlightRecord getFlightRecord(String flightRecordName) throws FlightRecordException {

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("flight_record_name", flightRecordName);

		FlightRecord flightRecord = null;
		try {
			flightRecord = jdbcTemplate.queryForObject(GET_FLIGHT_RECORD, namedParameters, new FlightRecordRowMapper());
			return flightRecord;
		} catch (EmptyResultDataAccessException erdae) {
			return null;
		} catch (DataAccessException dae) {
			logger.error("Failed to retrieve existing flight record matching specified identifier: {}", dae.getMessage(), dae);
			throw new FlightRecordException(new ApiError("FLIGHT_RECORD_RETRIEVAL_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}

	@Override
	public void updateFlightRecordOnAidStatus(String flightRecordName) throws FlightRecordException {

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("flight_record_name", flightRecordName);

		try {
			Integer returnVal = jdbcTemplate.update(UPDATE_FLIGHT_RECORD_DELETED_ON_AID, namedParameters);
			if (returnVal != 1) {
				logger.info("Failed to update deleted-on-AID status in flight record: {} record(s) updated", returnVal);
			}
		} catch (DataAccessException dae) {
			logger.error("Failed to update deleted on AID status of flight record: {}", dae.getMessage(), dae);
			throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPDATE_STATUS_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}
	
	private static final class FlightRecordRowMapper implements RowMapper<FlightRecord> {

		@Override
		public FlightRecord mapRow(ResultSet resultSet, int rowNum) throws SQLException {

			FlightRecord flightRecord = new FlightRecord(
					resultSet.getString("flight_record_name"),
					resultSet.getString("storage_path"),
					resultSet.getInt("file_size_kb"),
					OffsetDateTime.parse(resultSet.getString("flight_datetime"), Constants.DateTimeOffsetFormatterForParse).toInstant(),
					resultSet.getString("aid_id"),
					resultSet.getString("airline"),
					resultSet.getString("user_id"),
					resultSet.getString("status"),
					resultSet.getBoolean("upload_to_adw"),
					resultSet.getBoolean("deleted_on_aid"),
					resultSet.getBoolean("processed_by_analytics"),
					OffsetDateTime.parse(resultSet.getString("create_ts"), Constants.DateTimeOffsetFormatterForParse).toInstant(),
					OffsetDateTime.parse(resultSet.getString("update_ts"), Constants.DateTimeOffsetFormatterForParse).toInstant()
				);
			return flightRecord;
		}
	}

	private static final class FlightCountRowMapper implements RowMapper<FlightCount> {

		@Override
		public FlightCount mapRow(ResultSet resultSet, int rowNum) throws SQLException {

			String tailNumber = resultSet.getString("TAILNUMBER");
			// remove trailing / if present
			if (tailNumber != null && tailNumber.endsWith("/")) {
				tailNumber = tailNumber.substring(0, tailNumber.length() - 1);
			}
			int uploaded = resultSet.getInt("UPLOADED_BY_FDA");
			int processed = resultSet.getInt("PROCESSED_BY_ANALYTICS");
			String supaVersion = resultSet.getString("SUPA_VERSION");
			if (supaVersion == null || supaVersion.equals("")) {
				supaVersion = "unknown";
			}
			FlightCount flightCount = new FlightCount( tailNumber, uploaded, processed, supaVersion);
			return flightCount;
		}
	}

	private static final class SupaVersionRowMapper implements RowMapper<String> {

		@Override
		public String mapRow(ResultSet resultSet, int rowNum) throws SQLException {

			String supaVersion = resultSet.getString("SUPA_VERSION");
			if (supaVersion == null || supaVersion.equals("")) {
				supaVersion = "unknown";
			}
			return supaVersion;
		}
	}


}
