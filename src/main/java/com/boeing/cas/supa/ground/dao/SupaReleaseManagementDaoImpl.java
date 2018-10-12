package com.boeing.cas.supa.ground.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
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

import com.boeing.cas.supa.ground.exceptions.SupaReleaseException;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.SupaRelease;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

@Repository
public class SupaReleaseManagementDaoImpl implements SupaReleaseManagementDao {

	private final Logger logger = LoggerFactory.getLogger(SupaReleaseManagementDaoImpl.class);

	private static final String GET_SUPA_RELEASES = "SELECT * FROM supa_releases WHERE LOWER(airline) = :airline ORDER BY create_ts desc";
	private static final String GET_SUPA_RELEASE_BY_RELEASE = "SELECT * FROM supa_releases WHERE release = :release AND LOWER(airline) = :airline";
	private static final String GET_SUPA_RELEASE_BY_PART_NUMBER = "SELECT * FROM supa_releases WHERE part_number = :part_number AND LOWER(airline) = :airline";

	@Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

	@Override
	public List<SupaRelease> getSupaReleases(String airline) throws SupaReleaseException {

		List<SupaRelease> supaReleases = new ArrayList<>();
		Map<String,Object> namedParameters = new HashMap<>();
		//namedParameters.put("release", airline);
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
}
