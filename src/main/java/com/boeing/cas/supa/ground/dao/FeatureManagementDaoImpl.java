package com.boeing.cas.supa.ground.dao;

import com.boeing.cas.supa.ground.exceptions.FeatureManagementException;
import com.boeing.cas.supa.ground.pojos.*;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
public class FeatureManagementDaoImpl implements FeatureManagementDao {

	private final Logger logger = LoggerFactory.getLogger(FeatureManagementDaoImpl.class);

	private static final String FEATURE_MANAGEMENT_SELECT_SQL = "SELECT * FROM feature_management WHERE airline = :airline";
	private static final String AIRLINE_PREFERENCES_SELECT_DISPLAY_ONLY_SQL = "SELECT * FROM airline_preferences WHERE display = 1 and airline = :airline";
	private static final String AIRLINE_PREFERENCES_SELECT_SQL = "SELECT * FROM airline_preferences WHERE airline = :airline";
	private static final String USER_PREFERENCES_SELECT_SQL = "SELECT * FROM user_preferences WHERE airline = :airline";

	private static final String FEATURE_MANAGEMENT_PILOT_UPDATE_SQL
		= "UPDATE feature_management SET choice_pilot = :choice_pilot, "
		+ " updated_by = :updated_by WHERE airline = :airline and feature_key = :feature_key";

	private static final String FEATURE_MANAGEMENT_FOCAL_UPDATE_SQL
		= "UPDATE feature_management SET choice_focal = :choice_focal, "
		+ " updated_by = :updated_by WHERE airline = :airline and feature_key = :feature_key";

	private static final String FEATURE_MANAGEMENT_CHECK_AIRMAN_UPDATE_SQL
		= "UPDATE feature_management SET choice_check_airman = :choice_check_airman, "
		+ " updated_by = :updated_by WHERE airline = :airline and feature_key = :feature_key";

	private static final String FEATURE_MANAGEMENT_MAINTENANCE_UPDATE_SQL
		= "UPDATE feature_management SET choice_maintenance = :choice_maintenance, "
		+ " updated_by = :updated_by WHERE airline = :airline and feature_key = :feature_key";


	private static final String AIRLINE_PREFERENCES_PILOT_UPDATE_SQL
			= "UPDATE airline_preferences SET choice_pilot = :choice_pilot, "
			+ " updated_by = :updated_by WHERE airline = :airline and airline_key = :airline_key";

	private static final String AIRLINE_PREFERENCES_FOCAL_UPDATE_SQL
			= "UPDATE airline_preferences SET choice_focal = :choice_focal, "
			+ " updated_by = :updated_by WHERE airline = :airline and airline_key = :airline_key";

	private static final String AIRLINE_PREFERENCES_CHECK_AIRMAN_UPDATE_SQL
			= "UPDATE airline_preferences SET choice_check_airman = :choice_check_airman, "
			+ " updated_by = :updated_by WHERE airline = :airline and airline_key = :airline_key";

	private static final String AIRLINE_PREFERENCES_MAINTENANCE_UPDATE_SQL
			= "UPDATE airline_preferences SET choice_maintenance = :choice_maintenance, "
			+ " updated_by = :updated_by WHERE airline = :airline and airline_key = :airline_key";

	private static final String USER_PREFERENCES_UPDATE_SQL
			= "UPDATE user_preferences SET value = :value, "
			+ " updated_by = :updated_by WHERE airline = :airline and user_key = :user_key";



	@Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;


	@Override
	public void updateUserPreferences(String airline, String updatedBy, KeyValueUpdate keyValueUpdate)
			throws FeatureManagementException {

		Map<String,Object> namedParameters = new HashMap<>();

		namedParameters.put("user_key", keyValueUpdate.getKey());
		namedParameters.put("updated_by", updatedBy);
		namedParameters.put("value", keyValueUpdate.getValue());
		namedParameters.put("airline", airline);

		try {

			int returnVal = jdbcTemplate.update(USER_PREFERENCES_UPDATE_SQL, namedParameters);
			if (returnVal != 1) {
				logger.warn("Error occurred with update of key:{} and value:{} ",
						keyValueUpdate.getKey(), keyValueUpdate.getValue());
				logger.warn("Could not update user_preferences in database: {} record(s) updated", returnVal);
				throw new FeatureManagementException(new ApiError("USER_PREFERENCES_FAILURE", String.format("%d record(s) updated", returnVal), RequestFailureReason.INTERNAL_SERVER_ERROR));
			}
		}
		catch (DataAccessException dae) {

			logger.warn("Failed to update user preference record in database: {}", dae.getMessage(), dae);
			throw new FeatureManagementException(new ApiError("USER_PREFERENCES_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}


	@Override
	public void updateAirlinePreferences(String airline, String updatedBy, KeyValueUpdate keyValueUpdate)
			throws FeatureManagementException {

		Map<String,Object> namedParameters = new HashMap<>();
		String UPDATE_QUERY = "unknown";

		namedParameters.put("airline_key", keyValueUpdate.getKey());
		if (keyValueUpdate.getRole().equals("choiceFocal")) {
			namedParameters.put("choice_focal", keyValueUpdate.getValue());
			UPDATE_QUERY = AIRLINE_PREFERENCES_FOCAL_UPDATE_SQL;
		}
		if (keyValueUpdate.getRole().equals("choicePilot")) {
			namedParameters.put("choice_pilot", keyValueUpdate.getValue());
			UPDATE_QUERY = AIRLINE_PREFERENCES_PILOT_UPDATE_SQL;
		}
		if (keyValueUpdate.getRole().equals("choiceCheckAirman")) {
			namedParameters.put("choice_check_airman", keyValueUpdate.getValue());
			UPDATE_QUERY = AIRLINE_PREFERENCES_CHECK_AIRMAN_UPDATE_SQL;
		}
		if (keyValueUpdate.getRole().equals("choiceMaintenance")) {
			namedParameters.put("choice_maintenance", keyValueUpdate.getValue());
			UPDATE_QUERY = AIRLINE_PREFERENCES_MAINTENANCE_UPDATE_SQL;
		}
		namedParameters.put("updated_by", updatedBy);
		namedParameters.put("airline", airline);

		try {

			int returnVal = jdbcTemplate.update(UPDATE_QUERY, namedParameters);
			if (returnVal != 1) {
				logger.warn("Error occurred with update of key:{} role:{} and value:{} ",
						keyValueUpdate.getKey(), keyValueUpdate.getRole(), keyValueUpdate.getValue());
				logger.warn("Could not update airline_preferences in database: {} record(s) updated", returnVal);
				throw new FeatureManagementException(new ApiError("AIRLINE_PREFERENCES_FAILURE", String.format("%d record(s) updated", returnVal), RequestFailureReason.INTERNAL_SERVER_ERROR));
			}
		}
		catch (DataAccessException dae) {

			logger.warn("Failed to update airline preference record in database: {}", dae.getMessage(), dae);
			throw new FeatureManagementException(new ApiError("AIRLINE_PREFERENCES_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}


	@Override
	public void updateFeatureManagement(String airline, String updatedBy, KeyValueUpdate keyValueUpdate)
			throws FeatureManagementException {

		Map<String,Object> namedParameters = new HashMap<>();
		String UPDATE_QUERY = "unknown";

		namedParameters.put("feature_key", keyValueUpdate.getKey());
		if (keyValueUpdate.getRole().equals("choiceFocal")) {
			namedParameters.put("choice_focal", keyValueUpdate.getValue());
			UPDATE_QUERY = FEATURE_MANAGEMENT_FOCAL_UPDATE_SQL;
		}
		if (keyValueUpdate.getRole().equals("choicePilot")) {
			namedParameters.put("choice_pilot", keyValueUpdate.getValue());
			UPDATE_QUERY = FEATURE_MANAGEMENT_PILOT_UPDATE_SQL;
		}
		if (keyValueUpdate.getRole().equals("choiceCheckAirman")) {
			namedParameters.put("choice_check_airman", keyValueUpdate.getValue());
			UPDATE_QUERY = FEATURE_MANAGEMENT_CHECK_AIRMAN_UPDATE_SQL;
		}
		if (keyValueUpdate.getRole().equals("choiceMaintenance")) {
			namedParameters.put("choice_maintenance", keyValueUpdate.getValue());
			UPDATE_QUERY = FEATURE_MANAGEMENT_MAINTENANCE_UPDATE_SQL;
		}
		namedParameters.put("updated_by", updatedBy);
		namedParameters.put("airline", airline);

		try {

			int returnVal = jdbcTemplate.update(UPDATE_QUERY, namedParameters);
			if (returnVal != 1) {
				logger.warn("Error occurred with update of key:{} role:{} and value:{} ",
					keyValueUpdate.getKey(), keyValueUpdate.getRole(), keyValueUpdate.getValue());
				logger.warn("Could not update feature_management in database: {} record(s) updated", returnVal);
				throw new FeatureManagementException(new ApiError("FEATURE_MANAGEMENT_FAILURE", String.format("%d record(s) updated", returnVal), RequestFailureReason.INTERNAL_SERVER_ERROR));
			}
		}
		catch (DataAccessException dae) {

			logger.warn("Failed to update feature management record in database: {}", dae.getMessage(), dae);
			throw new FeatureManagementException(new ApiError("FEATURE_MANAGEMENT_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}


	@Override
	public List<FeatureManagement> getFeatureManagement(String airline) throws FeatureManagementException {

		List<FeatureManagement> featureManagement = new ArrayList<>();
		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("airline", airline);

		try {
			featureManagement = jdbcTemplate.query(FEATURE_MANAGEMENT_SELECT_SQL, namedParameters, new FeatureManagementRowMapper());
		} catch (DataAccessException dae) {
			logger.error("Failed to getFeatureManagement: {}", dae.getMessage(), dae);
			throw new FeatureManagementException(new ApiError("FEATURE_MANAGEMENT_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

		return featureManagement;
	}

	@Override
	public List<AirlinePreferences> getAirlinePreferences(String airline, boolean displayOnly) throws FeatureManagementException {

		List<AirlinePreferences> airlinePreferences = new ArrayList<>();
		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("airline", airline);

		try {
			if (displayOnly) {
				airlinePreferences = jdbcTemplate.query(AIRLINE_PREFERENCES_SELECT_DISPLAY_ONLY_SQL, namedParameters, new AirlinePreferencesRowMapper());
			} else {
				airlinePreferences = jdbcTemplate.query(AIRLINE_PREFERENCES_SELECT_SQL, namedParameters, new AirlinePreferencesRowMapper());
			}
		} catch (DataAccessException dae) {
			logger.error("Failed to getAirlinePreferences: {}", dae.getMessage(), dae);
			throw new FeatureManagementException(new ApiError("AIRLINE_PREFERENCES_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

		return airlinePreferences;
	}

	@Override
	public List<UserPreferences> getUserPreferences(String airline) throws FeatureManagementException {

		List<UserPreferences> userPreferences = new ArrayList<>();
		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("airline", airline);

		try {
			userPreferences = jdbcTemplate.query(USER_PREFERENCES_SELECT_SQL, namedParameters, new UserPreferencesRowMapper());
		} catch (DataAccessException dae) {
			logger.error("Failed to getUserPreferences: {}", dae.getMessage(), dae);
			throw new FeatureManagementException(new ApiError("USER_PREFERENCES_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

		return userPreferences;
	}


	private static final class FeatureManagementRowMapper implements RowMapper<FeatureManagement> {

		@Override
		public FeatureManagement mapRow(ResultSet resultSet, int rowNum) throws SQLException {

			FeatureManagement featureManagement = new FeatureManagement();

			featureManagement.setTitle(resultSet.getString("TITLE"));
			featureManagement.setDescription(resultSet.getString("DESCRIPTION"));
			featureManagement.setFeatureKey(resultSet.getString("FEATURE_KEY"));
			featureManagement.setChoicePilot(resultSet.getBoolean("CHOICE_PILOT"));
			featureManagement.setChoiceFocal(resultSet.getBoolean("CHOICE_FOCAL"));
			featureManagement.setChoiceCheckAirman(resultSet.getBoolean("CHOICE_CHECK_AIRMAN"));
			featureManagement.setChoiceMaintenance(resultSet.getBoolean("CHOICE_MAINTENANCE"));
			featureManagement.setUpdatedBy(resultSet.getString("UPDATED_BY"));
			featureManagement.setCreatedDateTime(resultSet.getString("CREATE_TS"));

			return featureManagement;
		}
	}

	private static final class AirlinePreferencesRowMapper implements RowMapper<AirlinePreferences> {

		@Override
		public AirlinePreferences mapRow(ResultSet resultSet, int rowNum) throws SQLException {

			AirlinePreferences airlinePreferences = new AirlinePreferences();

			airlinePreferences.setPreference(resultSet.getString("PREFERENCE"));
			airlinePreferences.setAirlineKey(resultSet.getString("AIRLINE_KEY"));
			airlinePreferences.setDescription(resultSet.getString("DESCRIPTION"));
			airlinePreferences.setChoicePilot(resultSet.getBoolean("CHOICE_PILOT"));
			airlinePreferences.setChoiceFocal(resultSet.getBoolean("CHOICE_FOCAL"));
			airlinePreferences.setChoiceCheckAirman(resultSet.getBoolean("CHOICE_CHECK_AIRMAN"));
			airlinePreferences.setChoiceMaintenance(resultSet.getBoolean("CHOICE_MAINTENANCE"));
			airlinePreferences.setUpdatedBy(resultSet.getString("UPDATED_BY"));
			airlinePreferences.setCreatedDateTime(resultSet.getString("CREATE_TS"));

			return airlinePreferences;
		}
	}

	private static final class UserPreferencesRowMapper implements RowMapper<UserPreferences> {

		@Override
		public UserPreferences mapRow(ResultSet resultSet, int rowNum) throws SQLException {

			UserPreferences userPreferences = new UserPreferences();

			userPreferences.setPreference(resultSet.getString("PREFERENCE"));
			userPreferences.setUserKey(resultSet.getString("USER_KEY"));
			userPreferences.setDescription(resultSet.getString("DESCRIPTION"));
			userPreferences.setGroupBy(resultSet.getString("GROUPBY"));
			userPreferences.setToggle(resultSet.getBoolean("TOGGLE"));
			userPreferences.setValue(resultSet.getString("VALUE"));
			userPreferences.setUpdatedBy(resultSet.getString("UPDATED_BY"));
			userPreferences.setCreatedDateTime(resultSet.getString("CREATE_TS"));

			return userPreferences;
		}
	}

}
