package com.boeing.cas.supa.ground.dao;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.boeing.cas.supa.ground.exceptions.UserAccountRegistrationException;
import com.boeing.cas.supa.ground.pojos.UserAccountRegistration;

@Repository
public class UserAccountRegistratioDaoImpl implements UserAccountRegistrationDao {

	private final Logger logger = LoggerFactory.getLogger(UserAccountRegistratioDaoImpl.class);

	private static final String USER_ACCOUNT_REGISTRATION_SQL = "INSERT INTO user_account_registrations (registration_token, user_object_id, user_principal_name, airline, work_email, account_state) VALUES (:registration_token, :user_object_id, :user_principal_name, :airline, :work_email, :account_state)";
	private static final String USER_ACCOUNT_ACTIVATION_PRECHECK_SQL = "SELECT COUNT(1) FROM user_account_registrations WHERE registration_token = :registration_token AND user_principal_name = :user_principal_name AND account_state = :account_state";
	private static final String USER_ACCOUNT_ACTIVATION_SQL = "UPDATE user_account_registrations SET account_state = :account_state_to WHERE registration_token = :registration_token AND user_principal_name = :user_principal_name AND account_state = :account_state_from";
	
	@Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

	@Override
	public void registerNewUserAccount(UserAccountRegistration userAccountRegistration) throws UserAccountRegistrationException {

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("registration_token", userAccountRegistration.getRegistrationToken());
		namedParameters.put("user_object_id", userAccountRegistration.getUserObjectId());
		namedParameters.put("user_principal_name", userAccountRegistration.getUserPrincipalName());
		namedParameters.put("airline", userAccountRegistration.getAirline());
		namedParameters.put("work_email", userAccountRegistration.getWorkEmail());
		namedParameters.put("account_state", userAccountRegistration.getAccountState());
		try {

			int returnVal = jdbcTemplate.update(USER_ACCOUNT_REGISTRATION_SQL, namedParameters);
			if (returnVal != 1) {
				logger.warn("Could not record user account registration in database: {} record(s) updated", returnVal);
				throw new UserAccountRegistrationException(String.format("%d record(s) updated", returnVal));
			}
		}
		catch (DataAccessException dae) {

			logger.warn("Failed to insert user account registration record in database: {}", dae.getMessage(), dae);
			throw new UserAccountRegistrationException("Database exception");
		}
	}

	@Override
	public boolean isUserAccountNotActivated(String registrationToken, String userPrincipalName, String accountState) throws UserAccountRegistrationException {

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("registration_token", registrationToken);
		namedParameters.put("user_principal_name", userPrincipalName);
		namedParameters.put("account_state", accountState);

		try {
			Integer count = jdbcTemplate.queryForObject(USER_ACCOUNT_ACTIVATION_PRECHECK_SQL, namedParameters, Integer.class);
			return count.intValue() > 0;
		}
		catch (DataAccessException dae) {

			logger.warn("Failed to determine user account activation status: {}", dae.getMessage(), dae);
			throw new UserAccountRegistrationException("Database exception");
		}
	}

	@Override
	public void enableNewUserAccount(String registrationToken, String userPrincipalName, String accountStateFrom, String accountStateTo)
			throws UserAccountRegistrationException {

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("registration_token", registrationToken);
		namedParameters.put("user_principal_name", userPrincipalName);
		namedParameters.put("account_state_from", accountStateFrom);
		namedParameters.put("account_state_to", accountStateTo);
		try {

			int returnVal = jdbcTemplate.update(USER_ACCOUNT_ACTIVATION_SQL, namedParameters);
			if (returnVal != 1) {
				logger.warn("Could not activate user account in database: {} record(s) updated", returnVal);
				throw new UserAccountRegistrationException(String.format("%d record(s) updated", returnVal));
			}
		}
		catch (DataAccessException dae) {

			logger.warn("Failed to activate user account registration record in database: {}", dae.getMessage(), dae);
			throw new UserAccountRegistrationException("Database exception");
		}

	}
}
