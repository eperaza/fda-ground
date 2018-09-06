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
import com.boeing.cas.supa.ground.pojos.SupaRelease;

@Repository
public class SupaReleaseManagementDaoImpl implements SupaReleaseManagementDao {

	private final Logger logger = LoggerFactory.getLogger(SupaReleaseManagementDaoImpl.class);

	private static final String GET_SUPA_RELEASES = "SELECT * FROM supa_releases ORDER BY create_ts desc";
	private static final String GET_SUPA_RELEASE_BY_RELEASE = "SELECT * FROM supa_releases WHERE release = :release";
	private static final String GET_SUPA_RELEASE_BY_PART_NUMBER = "SELECT * FROM supa_releases WHERE part_number = :part_number";

	@Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

	@Override
	public List<SupaRelease> getSupaReleases() throws SupaReleaseException {

		List<SupaRelease> supaReleases = new ArrayList<>();

		try {

			supaReleases = jdbcTemplate.query(GET_SUPA_RELEASES, new SupaReleaseRowMapper());
		} catch (DataAccessException dae) {
			logger.error("Failed to get all SUPA releases: {}", dae.getMessage(), dae);
			throw new SupaReleaseException("Database exception");
		}

		return supaReleases;
	}

	@Override
	public SupaRelease getSupaReleaseByRelease(String release) throws SupaReleaseException {

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("release", release);

		SupaRelease supaRelease = null;
		try {
			supaRelease = jdbcTemplate.queryForObject(GET_SUPA_RELEASE_BY_RELEASE, namedParameters, new SupaReleaseRowMapper());
			return supaRelease;
		} catch (EmptyResultDataAccessException erdae) {
			return null;
		} catch (DataAccessException dae) {
			logger.error("Failed to retrieve SUPA release matching specified release identifier: {}", dae.getMessage(), dae);
			throw new SupaReleaseException("Database exception");
		}
	}

	@Override
	public SupaRelease getSupaReleaseByPartNumber(String partNumber) throws SupaReleaseException {

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("part_number", partNumber);

		SupaRelease supaRelease = null;
		try {
			supaRelease = jdbcTemplate.queryForObject(GET_SUPA_RELEASE_BY_PART_NUMBER, namedParameters, new SupaReleaseRowMapper());
			return supaRelease;
		} catch (EmptyResultDataAccessException erdae) {
			return null;
		} catch (DataAccessException dae) {
			logger.error("Failed to retrieve SUPA release matching specified part number identifier: {}", dae.getMessage(), dae);
			throw new SupaReleaseException("Database exception");
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
