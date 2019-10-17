package com.boeing.cas.supa.ground.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.boeing.cas.supa.ground.exceptions.PowerBiInformationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.PowerBiInformation;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

@Repository
public class PowerBiInformationDaoImpl implements PowerBiInformationDao {

	private final Logger logger = LoggerFactory.getLogger(PowerBiInformationDaoImpl.class);

	private static final String GET_POWER_BI_INFORMATION = "SELECT * FROM power_bi_info WHERE LOWER(airline) = :airline ORDER BY createts desc";

	@Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;


	@Override
	public PowerBiInformation getPowerBiInformation(String airline) throws PowerBiInformationException {

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("airline", airline.toLowerCase());

		PowerBiInformation powerBiInformation = null;
		try {
			powerBiInformation = jdbcTemplate.queryForObject(GET_POWER_BI_INFORMATION, namedParameters, new PowerBiInformationRowMapper());
			return powerBiInformation;
		} catch (EmptyResultDataAccessException erdae) {
			logger.error("Failed to retrieve Power Bi information matching specified airline: {}", erdae.getMessage(), erdae);
			throw new PowerBiInformationException(new ApiError("POWER_BI_INFORMATION", "Missing or invalid airline identifier", RequestFailureReason.NOT_FOUND));
		} catch (DataAccessException dae) {
			logger.error("Failed to retrieve Power Bi information matching specified airline: {}", dae.getMessage(), dae);
			throw new PowerBiInformationException(new ApiError("POWER_BI_INFORMATION", "Failed to retrieve Power Bi information", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}

	private static final class PowerBiInformationRowMapper implements RowMapper<PowerBiInformation> {

		@Override
		public PowerBiInformation mapRow(ResultSet resultSet, int rowNum) throws SQLException {

			PowerBiInformation powerBiInformation = new PowerBiInformation(
				resultSet.getString("airline"),
				resultSet.getString("workspaceid"),
				resultSet.getString("reportid"),
				resultSet.getString("accesstoken"),
				resultSet.getString("accessexpiration"),
				resultSet.getString("embeddedtoken"),
				resultSet.getString("embeddedexpiration")
			);

			return powerBiInformation;
		}
	}
}
