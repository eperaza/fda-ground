package com.boeing.cas.supa.ground.dao;

import com.boeing.cas.supa.ground.exceptions.UserAccountRegistrationException;
import com.boeing.cas.supa.ground.pojos.ActivationCode;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.pojos.UserAccount;
import com.boeing.cas.supa.ground.pojos.UserAccountRegistration;

import java.util.List;

public interface UserAccountRegistrationDao {

	public List<UserAccount> getAllUsers(String airline, String userObjectId) throws UserAccountRegistrationException;

	public void updateUserAccount(User user) throws UserAccountRegistrationException;

	public void registerNewUserAccount(UserAccountRegistration userAccountRegistration)
			throws UserAccountRegistrationException;

	public boolean isUserAccountNotActivated(String registrationToken, String userPrincipalName, String accountState)
			throws UserAccountRegistrationException;

	public void enableNewUserAccount(String registrationToken, String userPrincipalName, String accountStateFrom,
			String accountStateTo) throws UserAccountRegistrationException;
	
	public void removeUserAccountRegistrationData(String userPrincipalName) throws UserAccountRegistrationException;


	public void insertActivationCode(String activation_code, String registration_cert, String airline) throws UserAccountRegistrationException;

	public List<ActivationCode> getActivationCode(String activation_code) throws UserAccountRegistrationException;

	public void removeActivationCode(String activation_code) throws UserAccountRegistrationException;

}
