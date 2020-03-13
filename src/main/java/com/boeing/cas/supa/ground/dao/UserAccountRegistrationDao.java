package com.boeing.cas.supa.ground.dao;

import com.boeing.cas.supa.ground.exceptions.UserAccountRegistrationException;
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


	public void insertRegistrationCode(String uuid, String registration_token, String airline) throws UserAccountRegistrationException;

	public String getRegistrationCode(String uuid) throws UserAccountRegistrationException;

	public void removeRegistrationCode(String uuid) throws UserAccountRegistrationException;

}
