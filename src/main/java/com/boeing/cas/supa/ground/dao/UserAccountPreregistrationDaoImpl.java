package com.boeing.cas.supa.ground.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.boeing.cas.supa.ground.pojos.*;
import com.boeing.cas.supa.ground.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.boeing.cas.supa.ground.exceptions.UserAccountRegistrationException;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

@Repository
public class UserAccountPreregistrationDaoImpl implements UserAccountPreregistrationDao {

	private final Logger logger = LoggerFactory.getLogger(UserAccountPreregistrationDaoImpl.class);

	private static final String USER_ACCOUNT_REGISTRATION_SQL = "INSERT INTO user_account_preregistrations (user_id, first, last, email, role, account_state, airline) VALUES (:user_id, :first, :last, :email, :role, :account_state, :airline)";
	
	private static final String USER_ACCOUNT_SELECT_SQL = "SELECT * FROM user_account_preregistrations WHERE airline = :airline";
	private static final String USER_ACCOUNT_REMOVAL_SQL = "DELETE FROM user_account_preregistrations WHERE user_id = :user_id";
    private static final String USER_ACCOUNT_UPDATE_SQL
			= "UPDATE user_account_preregistrations SET user_id = :user_id, first = :first, last = :last, "
			+ " email = :email, role = :role "
			+ " WHERE user_id = :user_id AND airline = :airline";

	@Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Override
	public List<PreUserAccount> getAllUsers(String airline) throws UserAccountRegistrationException {

		List<PreUserAccount> users = new ArrayList<>();
		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("airline", airline);

		try {
			users = jdbcTemplate.query(USER_ACCOUNT_SELECT_SQL, namedParameters, new UserRowMapper());
		} catch (DataAccessException dae) {
			logger.error("Failed to getAllUserAccounts: {}", dae.getMessage(), dae);
			throw new UserAccountRegistrationException(new ApiError("SELECT_USER_ACCOUNT_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

		return users;
	}

    @Override
	public int registerNewUserAccount(PreUserAccount userAccountRegistration) throws UserAccountRegistrationException {

        int returnVal = 0;

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("user_id", userAccountRegistration.getUserId());
		namedParameters.put("first", userAccountRegistration.getFirst());
		namedParameters.put("last", userAccountRegistration.getLast());
		namedParameters.put("airline", userAccountRegistration.getAirline());
		namedParameters.put("email", userAccountRegistration.getEmail());
		namedParameters.put("account_state", userAccountRegistration.getAccountState());
        namedParameters.put("role", userAccountRegistration.getRole());

		try {

			returnVal = jdbcTemplate.update(USER_ACCOUNT_REGISTRATION_SQL, namedParameters);
			if (returnVal != 1) {
				logger.warn("Could not record user account registration in database: {} record(s) updated", returnVal);
				throw new UserAccountRegistrationException(new ApiError("CREATE_USER_FAILURE", String.format("%d record(s) updated", returnVal), RequestFailureReason.INTERNAL_SERVER_ERROR));
			}
		}
		catch (DataAccessException dae) {
			logger.warn("Failed to insert user account registration record in database: {}", dae.getMessage(), dae);
			throw new UserAccountRegistrationException(new ApiError("CREATE_USER_FAILURE", dae.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

        return returnVal;
	}

    @Override
	public int removeUserAccountRegistrationData(String userId) throws UserAccountRegistrationException {
        int returnVal = 0;
		Map<String,Object> namedParameters = new HashMap<>();
		logger.debug("removing: {}", userId);
		namedParameters.put("user_id", userId);
		try {
			returnVal = jdbcTemplate.update(USER_ACCOUNT_REMOVAL_SQL, namedParameters);
			if (returnVal != 1) {
				logger.warn("Could not remove user account in database: {} record(s) updated", returnVal);
				throw new UserAccountRegistrationException(new ApiError("DELETE_USER_ACCOUNT_FAILURE", String.format("%d record(s) removed", returnVal), RequestFailureReason.INTERNAL_SERVER_ERROR));
			}
		}
		catch (DataAccessException dae) {

			logger.warn("Failed to remove user account registration records in database: {}", dae.getMessage(), dae);
			throw new UserAccountRegistrationException(new ApiError("DELETE_USER_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

        return returnVal;
	}

    @Override
	public int updateUserAccount(PreUserAccount user) throws UserAccountRegistrationException {

		int returnVal = 0;
		Map<String,Object> namedParameters = new HashMap<>();

		namedParameters.put("user_id", user.getUserId());
		namedParameters.put("airline", user.getAirline());
		namedParameters.put("first", user.getFirst());
		namedParameters.put("last", user.getLast());
		namedParameters.put("email", user.getEmail());
		namedParameters.put("role", user.getRole());
		
		try {

			returnVal = jdbcTemplate.update(USER_ACCOUNT_UPDATE_SQL, namedParameters);
			if (returnVal != 1) {
				logger.warn("Could not update user account in database: {} record(s) updated", returnVal);
				throw new UserAccountRegistrationException(new ApiError("UPDATE_USER_ACCOUNT_FAILURE", String.format("%d record(s) updated", returnVal), RequestFailureReason.INTERNAL_SERVER_ERROR));
			}
			logger.warn("Updated {} in database: {} record(s) updated", user.getUserId(), returnVal);

		}
		catch (DataAccessException dae) {

			logger.warn("Failed to update user account registration record in database: {}", dae.getMessage(), dae);
			throw new UserAccountRegistrationException(new ApiError("UPDATE_USER_ACCOUNT_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
		return returnVal;
	}
    
    private static final class UserRowMapper implements RowMapper<PreUserAccount> {

		@Override
		public PreUserAccount mapRow(ResultSet resultSet, int rowNum) throws SQLException {

			PreUserAccount user = new PreUserAccount();
			user.setUserId(resultSet.getString("USER_ID"));
			user.setFirst(resultSet.getString("FIRST"));
			user.setLast(resultSet.getString("LAST"));
            user.setAirline(resultSet.getString("AIRLINE"));
            user.setRole(resultSet.getString("ROLE"));
            user.setEmail(resultSet.getString("EMAIL"));
			String account_state = resultSet.getString("ACCOUNT_STATE");
			user.setAccountState(account_state.equals("USER_ACTIVATED")?"true":"false");

			return user;
		}
	}


}