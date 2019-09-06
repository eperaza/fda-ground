package com.boeing.cas.supa.ground.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.boeing.cas.supa.ground.pojos.CurrentSupaRelease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.boeing.cas.supa.ground.exceptions.SupaReleaseException;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.SupaRelease;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

@Repository
public class SupaReleaseManagementDaoImpl implements SupaReleaseManagementDao {

	private final Logger logger = LoggerFactory.getLogger(SupaReleaseManagementDaoImpl.class);

	private static final String GET_SUPA_RELEASES = "SELECT * FROM supa_releases WHERE LOWER(airline) = :airline ORDER BY create_ts desc";
	private static final String GET_WAR_RELEASES = "SELECT * FROM war_releases WHERE LOWER(airline) = :airline ORDER BY create_ts desc";
	private static final String GET_SUPA_RELEASE_BY_RELEASE = "SELECT * FROM supa_releases WHERE release = :release AND LOWER(airline) = :airline";
	private static final String GET_WAR_RELEASE_BY_RELEASE = "SELECT * FROM war_releases WHERE supa_release = :release AND LOWER(airline) = :airline";
	private static final String GET_SUPA_RELEASE_BY_PART_NUMBER = "SELECT * FROM supa_releases WHERE part_number = :part_number AND LOWER(airline) = :airline";

	private static final String REMOVE_CURRENT_SUPA_RELEASE = "DELETE FROM current_supa_releases WHERE LOWER(airline) = :airline";
	private static final String INSERT_CURRENT_SUPA_RELEASE = "INSERT INTO current_supa_releases (airline, release, description, updated_by) VALUES (:airline, :release, :description, :updated_by)";
    private static final String GET_CURRENT_SUPA_RELEASE_BY_CUSTOMER = "SELECT * FROM current_supa_releases WHERE LOWER(airline) = :airline ORDER BY create_ts desc";

	@Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

	@Override
	public void insertCurrentSupaRelease(CurrentSupaRelease currentSupaRelease) throws SupaReleaseException {

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("airline", currentSupaRelease.getAirline());
		namedParameters.put("release", currentSupaRelease.getRelease());
		namedParameters.put("description", currentSupaRelease.getDescription());
		namedParameters.put("updated_by", currentSupaRelease.getUpdatedBy());
		try {

			int returnVal = jdbcTemplate.update(INSERT_CURRENT_SUPA_RELEASE, namedParameters);
			if (returnVal != 1) {
				logger.error("Could not insert current supa release into database: {} record(s) updated", returnVal);
				throw new SupaReleaseException(new ApiError("SUPA_RELEASE_MGMT", String.format("%d record(s) updated", returnVal), RequestFailureReason.INTERNAL_SERVER_ERROR));
			}
		} catch (DataAccessException dae) {
			logger.error("Failed to current supa release data into database: {}", dae.getMessage(), dae);
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_MGMT", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}

	@Override
	public void removeCurrentSupaRelease(String airline) throws SupaReleaseException {

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("airline", airline);
		try {
			jdbcTemplate.update(REMOVE_CURRENT_SUPA_RELEASE, namedParameters);
		}
		catch (DataAccessException dae) {

			logger.warn("Failed to current supa version for airline [{}]",  airline);
			logger.warn("SQL Exception: " + dae.getMessage());
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_MGMT", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}

	@Override
	public List<CurrentSupaRelease> getCurrentSupaRelease(String airline) throws SupaReleaseException {

		List<CurrentSupaRelease> supaReleases = new ArrayList<>();
		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("airline", airline);

		try {
			logger.debug(GET_CURRENT_SUPA_RELEASE_BY_CUSTOMER + " using [" + namedParameters.get("airline")+ "]");
			supaReleases = jdbcTemplate.query(GET_CURRENT_SUPA_RELEASE_BY_CUSTOMER, namedParameters, new CurrentSupaReleaseRowMapper());
		} catch (DataAccessException dae) {
			logger.error("Failed to get Current SUPA release: {}", dae.getMessage(), dae);
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_MGMT", "Failed to retrieve Current SUPA release", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
		return supaReleases;
	}

	@Override
	public List<SupaRelease> getSupaReleases(String airline) throws SupaReleaseException {

		List<SupaRelease> supaReleases = new ArrayList<>();
		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("airline", airline);

		try {
			logger.debug(GET_SUPA_RELEASES + " using [" + namedParameters.get("airline")+ "]");
			supaReleases = jdbcTemplate.query(GET_SUPA_RELEASES, namedParameters, new SupaReleaseRowMapper());
		} catch (DataAccessException dae) {
			logger.error("Failed to get all SUPA releases: {}", dae.getMessage(), dae);
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_MGMT", "Failed to retrieve SUPA releases", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

		return supaReleases;
	}

	@Override
	public List<SupaRelease> getWarReleases(String airline) throws SupaReleaseException {

		List<SupaRelease> warReleases = new ArrayList<>();
		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("airline", airline);

		try {
			logger.debug(GET_WAR_RELEASES + " using [" + namedParameters.get("airline")+ "]");
			warReleases = jdbcTemplate.query(GET_WAR_RELEASES, namedParameters, new WarReleaseRowMapper());
		} catch (DataAccessException dae) {
			logger.error("Failed to get all WAR releases: {}", dae.getMessage(), dae);
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_MGMT", "Failed to retrieve WAR releases", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

		return warReleases;
	}

	@Override
	public SupaRelease getSupaReleaseByRelease(String release, String airline) throws SupaReleaseException {

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("release", release);
		namedParameters.put("airline", airline);

		SupaRelease supaRelease = null;
		try {
			supaRelease = jdbcTemplate.queryForObject(GET_SUPA_RELEASE_BY_RELEASE, namedParameters, new SupaReleaseRowMapper());
			return supaRelease;
		} catch (EmptyResultDataAccessException erdae) {
			logger.error("Failed to retrieve SUPA release matching specified release identifier: {}", erdae.getMessage(), erdae);
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_MGMT", "Missing or invalid SUPA release", RequestFailureReason.NOT_FOUND));
		} catch (DataAccessException dae) {
			logger.error("Failed to retrieve SUPA release matching specified release identifier: {}", dae.getMessage(), dae);
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_MGMT", "Failed to retrieve SUPA release", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}

	@Override
	public SupaRelease getWarReleaseByRelease(String release, String airline) throws SupaReleaseException {

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("release", release);
		namedParameters.put("airline", airline);

		SupaRelease supaRelease = null;
		try {
			supaRelease = jdbcTemplate.queryForObject(GET_WAR_RELEASE_BY_RELEASE, namedParameters, new WarReleaseRowMapper());
			return supaRelease;
		} catch (EmptyResultDataAccessException erdae) {
			logger.error("Failed to retrieve SUPA release matching specified release identifier: {}", erdae.getMessage(), erdae);
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_MGMT", "Missing or invalid (WAR) release [" + release + "] for airline: " + airline, RequestFailureReason.NOT_FOUND));
		} catch (DataAccessException dae) {
			logger.error("Failed to retrieve SUPA release matching specified release identifier: {}", dae.getMessage(), dae);
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_MGMT", "Failed to retrieve WAR release for airline", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}

	@Override
	public SupaRelease getSupaReleaseByPartNumber(String partNumber, String airline) throws SupaReleaseException {

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("part_number", partNumber);
		namedParameters.put("airline", airline);

		SupaRelease supaRelease = null;
		try {
			supaRelease = jdbcTemplate.queryForObject(GET_SUPA_RELEASE_BY_PART_NUMBER, namedParameters, new SupaReleaseRowMapper());
			return supaRelease;
		} catch (EmptyResultDataAccessException erdae) {
			logger.error("Failed to retrieve SUPA release matching specified release identifier: {}", erdae.getMessage(), erdae);
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_MGMT", "Missing or invalid SUPA release", RequestFailureReason.NOT_FOUND));
		} catch (DataAccessException dae) {
			logger.error("Failed to retrieve SUPA release matching specified part number identifier: {}", dae.getMessage(), dae);
			throw new SupaReleaseException(new ApiError("SUPA_RELEASE_MGMT", "Failed to retrieve SUPA release", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}

	private static final class SupaReleaseRowMapper implements RowMapper<SupaRelease> {

		@Override
		public SupaRelease mapRow(ResultSet resultSet, int rowNum) throws SQLException {

			SupaRelease supaRelease = new SupaRelease(
					resultSet.getString("release"),
					resultSet.getString("part_number"),
					resultSet.getString("path"),
					resultSet.getString("airline")
				);

			return supaRelease;
		}
	}

	private static final class CurrentSupaReleaseRowMapper implements RowMapper<CurrentSupaRelease> {

		@Override
		public CurrentSupaRelease mapRow(ResultSet resultSet, int rowNum) throws SQLException {

			CurrentSupaRelease supaRelease = new CurrentSupaRelease(
					resultSet.getString("release"),
					resultSet.getString("description"),
					resultSet.getString("release_date"),
					resultSet.getString("updated_by"),
					resultSet.getString("airline")
			);
			return supaRelease;
		}
	}

	private static final class WarReleaseRowMapper implements RowMapper<SupaRelease> {

		@Override
		public SupaRelease mapRow(ResultSet resultSet, int rowNum) throws SQLException {

			SupaRelease supaRelease = new SupaRelease(
					resultSet.getString("supa_release"),
					resultSet.getString("supa_part_number"),
					resultSet.getString("path"),
					resultSet.getString("airline")
			);

			return supaRelease;
		}
	}

}
