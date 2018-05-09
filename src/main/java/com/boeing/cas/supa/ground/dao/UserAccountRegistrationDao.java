package com.boeing.cas.supa.ground.dao;

import com.boeing.cas.supa.ground.exceptions.UserAccountRegistrationException;
import com.boeing.cas.supa.ground.pojos.UserAccountRegistration;

public interface UserAccountRegistrationDao {

	public void registerNewUserAccount(UserAccountRegistration userAccountRegistration) throws UserAccountRegistrationException;
	
	public void enableNewUserAccount(String registrationToken) throws UserAccountRegistrationException;
}
