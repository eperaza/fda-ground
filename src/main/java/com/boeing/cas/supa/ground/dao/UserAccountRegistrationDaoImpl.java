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
public class UserAccountRegistrationDaoImpl implements UserAccountRegistrationDao {

	private final Logger logger = LoggerFactory.getLogger(UserAccountRegistrationDaoImpl.class);

	private static final String USER_ACCOUNT_REGISTRATION_SQL = "INSERT INTO user_account_registrations (registration_token, user_object_id, user_principal_name, airline, work_email, account_state) VALUES (:registration_token, :user_object_id, :user_principal_name, :airline, :work_email, :account_state)";
	private static final String USER_ACCOUNT_ACTIVATION_PRECHECK_SQL = "SELECT COUNT(1) FROM user_account_registrations WHERE registration_token = :registration_token AND user_principal_name = :user_principal_name AND account_state = :account_state";
	private static final String USER_ACCOUNT_ACTIVATION_SQL = "UPDATE user_account_registrations SET account_state = :account_state_to WHERE registration_token = :registration_token AND user_principal_name = :user_principal_name AND account_state = :account_state_from";
	private static final String USER_ACCOUNT_REMOVAL_SQL = "DELETE FROM user_account_registrations WHERE user_principal_name = :user_principal_name";

	private static final String USER_ACCOUNT_SELECT_SQL = "SELECT * FROM user_account_registrations WHERE airline = :airline AND user_object_id != :user_object_id";
	private static final String USER_ACCOUNT_UPDATE_SQL
		= "UPDATE user_account_registrations SET display_name = :display_name, first_name = :first_name, last_name = :last_name, "
		+ " email_address = :email_address, user_role = :user_role, registration_date = :registration_date "
		+ " WHERE user_object_id = :user_object_id AND airline = :airline";

	private static final String USER_ACTIVATION_CODE_SQL_INSERT = "INSERT INTO user_activation_codes (email_address, activation_code, registration_cert, airline) VALUES (:email_address, :activation_code, :registration_cert, :airline)";
	private static final String USER_ACTIVATION_CODE_SQL_SELECT = "SELECT * FROM user_activation_codes WHERE activation_code = :activation_code AND email_address = :email_address";
	private static final String USER_ACTIVATION_CODE_SQL_DELETE = "DELETE FROM user_activation_codes WHERE activation_code = :activation_code AND email_address = :email_address";

	@Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

	@Override
	public List<UserAccount> getAllUsers(String airline, String userObjectId) throws UserAccountRegistrationException {

		List<UserAccount> users = new ArrayList<>();
		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("airline", airline);
		namedParameters.put("user_object_id", userObjectId);

		try {
			users = jdbcTemplate.query(USER_ACCOUNT_SELECT_SQL, namedParameters, new UserRowMapper());
		} catch (DataAccessException dae) {
			logger.error("Failed to getAllUserAccounts: {}", dae.getMessage(), dae);
			throw new UserAccountRegistrationException(new ApiError("SELECT_USER_ACCOUNT_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

		return users;
	}


	@Override
	public void updateUserAccount(User user) throws UserAccountRegistrationException {

		Map<String,Object> namedParameters = new HashMap<>();

		List<Group> grps = user.getGroups();
		String airline = "airline-unknown";
		String role = "role-unknown";
		if (grps != null && grps.size() == 2)
		{
			for (Group gp : grps) {
				if (gp.getDisplayName().toLowerCase().startsWith("airline-")) airline = gp.getDisplayName();
				if (gp.getDisplayName().toLowerCase().startsWith("role-")) role = gp.getDisplayName();
			}
		}
		namedParameters.put("user_object_id", user.getObjectId());
		namedParameters.put("airline", airline);
		namedParameters.put("display_name", user.getDisplayName());
		namedParameters.put("first_name", user.getGivenName());
		namedParameters.put("last_name", user.getSurname());
		if (user.getOtherMails() != null && !user.getOtherMails().isEmpty()) {
			namedParameters.put("email_address", user.getOtherMails().get(0));
		} else {
			namedParameters.put("email_address", "unknown");
		}
		namedParameters.put("user_role", role);
		if (user.getCreatedDateTime() != null && !user.getCreatedDateTime().isEmpty()) {
			namedParameters.put("registration_date", user.getCreatedDateTime());
		}
		else {
			Calendar cNow = Calendar.getInstance();
			SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
			String date_time = date.format(cNow.getTime()) + "T" + time.format(cNow.getTime()) + "Z";
			namedParameters.put("registration_date", date_time);
		}
		try {

			int returnVal = jdbcTemplate.update(USER_ACCOUNT_UPDATE_SQL, namedParameters);
			if (returnVal != 1) {
				logger.warn("Could not update user account in database: {} record(s) updated", returnVal);
				throw new UserAccountRegistrationException(new ApiError("UPDATE_USER_ACCOUNT_FAILURE", String.format("%d record(s) updated", returnVal), RequestFailureReason.INTERNAL_SERVER_ERROR));
			}
		}
		catch (DataAccessException dae) {

			logger.warn("Failed to update user account registration record in database: {}", dae.getMessage(), dae);
			throw new UserAccountRegistrationException(new ApiError("UPDATE_USER_ACCOUNT_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}


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
				throw new UserAccountRegistrationException(new ApiError("CREATE_USER_FAILURE", String.format("%d record(s) updated", returnVal), RequestFailureReason.INTERNAL_SERVER_ERROR));
			}
		}
		catch (DataAccessException dae) {

			logger.warn("Failed to insert user account registration record in database: {}", dae.getMessage(), dae);
			throw new UserAccountRegistrationException(new ApiError("CREATE_USER_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
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
			throw new UserAccountRegistrationException(new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
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
				throw new UserAccountRegistrationException(new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", String.format("%d record(s) updated", returnVal), RequestFailureReason.INTERNAL_SERVER_ERROR));
			}
		}
		catch (DataAccessException dae) {

			logger.warn("Failed to activate user account registration record in database: {}", dae.getMessage(), dae);
			throw new UserAccountRegistrationException(new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}

	@Override
	public void removeUserAccountRegistrationData(String userPrincipalName) throws UserAccountRegistrationException {

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("user_principal_name", userPrincipalName);
		try {
			jdbcTemplate.update(USER_ACCOUNT_REMOVAL_SQL, namedParameters);
		}
		catch (DataAccessException dae) {

			logger.warn("Failed to remove user account registration records in database: {}", dae.getMessage(), dae);
			throw new UserAccountRegistrationException(new ApiError("DELETE_USER_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}


	@Override
	public void insertActivationCode(String email_address, String activation_code, String registration_cert, String airline)
			throws UserAccountRegistrationException
	{

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("email_address", email_address);
		namedParameters.put("activation_code", activation_code);
		namedParameters.put("registration_cert", registration_cert);
		namedParameters.put("airline", airline);
		try {

			int returnVal = jdbcTemplate.update(USER_ACTIVATION_CODE_SQL_INSERT, namedParameters);
			if (returnVal != 1) {
				logger.warn("Could not insert activation code into database: {} record(s) updated", returnVal);
				throw new UserAccountRegistrationException(new ApiError("CREATE_USER_FAILURE", String.format("%d record(s) updated", returnVal), RequestFailureReason.INTERNAL_SERVER_ERROR));
			}
		}
		catch (DataAccessException dae) {

			logger.warn("Failed to insert activation code into database: {}", dae.getMessage(), dae);
			throw new UserAccountRegistrationException(new ApiError("CREATE_USER_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}

	@Override
	public List<ActivationCode> getActivationCode(String email_address, String activation_code) throws UserAccountRegistrationException {

		List<ActivationCode> codes = new ArrayList<>();

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("email_address", email_address);
		namedParameters.put("activation_code", activation_code);

		try {
			codes = jdbcTemplate.query(USER_ACTIVATION_CODE_SQL_SELECT, namedParameters, new ActivationCodeMapper());
			return codes;
		}
		catch (DataAccessException dae) {

			logger.warn("Failed to obtain activation code: {}", dae.getMessage(), dae);
			throw new UserAccountRegistrationException(new ApiError("REGISTER_USER_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}

	@Override
	public void removeActivationCode(String email_address, String activation_code) throws UserAccountRegistrationException {

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("email_address", email_address);
		namedParameters.put("activation_code", activation_code);
		try {
			jdbcTemplate.update(USER_ACTIVATION_CODE_SQL_DELETE, namedParameters);
		}
		catch (DataAccessException dae) {

			logger.warn("Failed to remove activation code in database: {}", dae.getMessage(), dae);
			throw new UserAccountRegistrationException(new ApiError("REGISTER_USER_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}


	private static final class ActivationCodeMapper implements RowMapper<ActivationCode> {

		@Override
		public ActivationCode mapRow(ResultSet resultSet, int rowNum) throws SQLException {

			ActivationCode activationCode = new ActivationCode();
				activationCode.setEmailAddress(resultSet.getString("EMAIL_ADDRESS"));
				activationCode.setActivationCode(resultSet.getString("ACTIVATION_CODE"));
				activationCode.setRegistrationCert(resultSet.getString("REGISTRATION_CERT"));
				activationCode.setAirline(resultSet.getString("AIRLINE"));

			return activationCode;
		}
	}

	private static final class UserRowMapper implements RowMapper<UserAccount> {

		@Override
		public UserAccount mapRow(ResultSet resultSet, int rowNum) throws SQLException {

			UserAccount user = new UserAccount();
				user.setObjectType("User");
				user.setObjectId(resultSet.getString("USER_OBJECT_ID"));
				user.setUserPrincipalName(resultSet.getString("USER_PRINCIPAL_NAME"));
				String account_state = resultSet.getString("ACCOUNT_STATE");
				user.setAccountEnabled(account_state.equals("USER_ACTIVATED")?"true":"false");

				user.setDisplayName(resultSet.getString("DISPLAY_NAME"));
				user.setGivenName(resultSet.getString("FIRST_NAME"));
				user.setSurname(resultSet.getString("LAST_NAME"));
				int endPoint = user.getUserPrincipalName().indexOf("@");
				if (endPoint > 0) {
					user.setMailNickname(user.getUserPrincipalName().substring(0, endPoint));
				}
				String other_email = resultSet.getString("EMAIL_ADDRESS");
				List<String> emails = new ArrayList<>();
				emails.add(other_email);

				user.setOtherMails(emails);
				user.setUserRole(resultSet.getString("USER_ROLE"));
				user.setCreatedDateTime(resultSet.getString("REGISTRATION_DATE"));
			return user;
		}
	}

}
