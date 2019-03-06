package com.boeing.cas.supa.ground.dao;

import com.boeing.cas.supa.ground.exceptions.UserAccountRegistrationException;
import com.boeing.cas.supa.ground.pojos.UserAccountRegistration;

public interface UserAccountRegistrationDao {

//	public void insertRegistrationCode(String uuid, String message, String user_principal_name, String airline)
//			throws UserAccountRegistrationException;
//
//	public String getRegistrationCode(String uuid)
//			throws UserAccountRegistrationException;
//
//	public void removeRegistrationCode(String uuid)
//			throws UserAccountRegistrationException;
//
	public void registerNewUserAccount(UserAccountRegistration userAccountRegistration)
			throws UserAccountRegistrationException;

	public boolean isUserAccountNotActivated(String registrationToken, String userPrincipalName, String accountState)
			throws UserAccountRegistrationException;

	public void enableNewUserAccount(String registrationToken, String userPrincipalName, String accountStateFrom,
			String accountStateTo) throws UserAccountRegistrationException;
	
	public void removeUserAccountRegistrationData(String userPrincipalName) throws UserAccountRegistrationException;
}
