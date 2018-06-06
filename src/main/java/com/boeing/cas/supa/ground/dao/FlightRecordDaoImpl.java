package com.boeing.cas.supa.ground.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.boeing.cas.supa.ground.exceptions.FlightRecordException;
import com.boeing.cas.supa.ground.pojos.FlightRecord;
import com.boeing.cas.supa.ground.utils.Constants;

import microsoft.sql.DateTimeOffset;

@Repository
public class FlightRecordDaoImpl implements FlightRecordDao {

	private final Logger logger = LoggerFactory.getLogger(FlightRecordDaoImpl.class);

	private static final String GET_FLIGHT_RECORD = "SELECT * FROM flight_records WHERE flight_record_name = :flight_record_name";
	private static final String GET_FLIGHT_RECORDS = "SELECT * FROM flight_records WHERE airline = :airline AND deleted_on_aid = 0 ORDER BY create_ts desc";
	private static final String INSERT_FLIGHT_RECORD = "INSERT INTO flight_records (flight_record_name, storage_path, file_size_kb, flight_datetime, aid_id, airline, user_id, upload_to_adw) VALUES (:flight_record_name, :storage_path, :file_size_kb, :flight_datetime, :aid_id, :airline, :user_id, :upload_to_adw)";
	private static final String UPDATE_FLIGHT_RECORD_DELETED_ON_AID = "UPDATE flight_records SET deleted_on_aid = 1 WHERE flight_record_name = :flight_record_name";

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
				throw new FlightRecordException(String.format("%d record(s) updated", returnVal));
			}
		}
		catch (DataAccessException dae) {
			logger.error("Failed to insert flight data into database: {}", dae.getMessage(), dae);
			throw new FlightRecordException("Database exception");
		}
	}

	@Override
	public List<FlightRecord> getAllFlightRecords(String airline) throws FlightRecordException {

		List<FlightRecord> flightRecords = new ArrayList<>();
		try {

			flightRecords = jdbcTemplate.query(GET_FLIGHT_RECORDS, new FlightRecordRowMapper());
		} catch (DataAccessException dae) {
			logger.error("Failed to getAllFlightRecords: {}", dae.getMessage(), dae);
			throw new FlightRecordException("Database exception");
		}

		return flightRecords;
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
			throw new FlightRecordException("Database exception");
		}
	}

	@Override
	public void removeFlightRecord(String flightRecordName) throws FlightRecordException {

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("flight_record_name", flightRecordName);

		try {
			Integer returnVal = jdbcTemplate.update(UPDATE_FLIGHT_RECORD_DELETED_ON_AID, namedParameters);
			if (returnVal != 1) {
				logger.warn("Failed to update deleted on AID of flight record: {} record(s) updated", returnVal);
				throw new FlightRecordException(String.format("%d record(s) updated", returnVal));
			}
		}
		catch (DataAccessException dae) {

			logger.error("Failed to update deleted on AID status of flight record: {}", dae.getMessage(), dae);
			throw new FlightRecordException("Database exception");
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
					resultSet.getBoolean("processed_by_analytics")
				);

			System.out.println(flightRecord);
			
			return flightRecord;
		}
	}
}
