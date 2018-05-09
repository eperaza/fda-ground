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

	private static final String NEW_USER_ACCOUNT_REG_SQL = "INSERT INTO user_account_registrations (registration_token, user_object_id, user_principal_name, airline, work_email) VALUES (:registration_token, :user_object_id, :user_principal_name, :airline, :work_email)";
	
	@Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

	@Override
	public void registerNewUserAccount(UserAccountRegistration userAccountRegistration) throws UserAccountRegistrationException {

		//jdbcTemplate.update(NEW_USER_ACCOUNT_REG_SQL, user);
		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("registration_token", userAccountRegistration.getRegistrationToken());
		namedParameters.put("user_object_id", userAccountRegistration.getUserObjectId());
		namedParameters.put("user_principal_name", userAccountRegistration.getUserPrincipalName());
		namedParameters.put("airline", userAccountRegistration.getAirline());
		namedParameters.put("work_email", userAccountRegistration.getWorkEmail());
		try {
			
			int returnVal = jdbcTemplate.update(NEW_USER_ACCOUNT_REG_SQL, namedParameters);
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
	public void enableNewUserAccount(String registrationToken) throws UserAccountRegistrationException {
		// TODO Auto-generated method stub
	}
}
