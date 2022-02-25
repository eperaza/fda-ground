package com.boeing.cas.supa.ground.dao;

import com.boeing.cas.supa.ground.exceptions.UserAccountRegistrationException;
import com.boeing.cas.supa.ground.pojos.ActivationCode;
import com.boeing.cas.supa.ground.pojos.PreUserAccount;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.pojos.UserAccount;
import com.boeing.cas.supa.ground.pojos.UserAccountRegistration;

import java.util.List;

public interface UserAccountPreregistrationDao {

	public List<PreUserAccount> getAllUsers(String airline) throws UserAccountRegistrationException;
	public int removeUserAccountRegistrationData(String userId) throws UserAccountRegistrationException;
	public int registerNewUserAccount(PreUserAccount userAccountRegistration) throws UserAccountRegistrationException;
	public int updateUserAccount(PreUserAccount user) throws UserAccountRegistrationException;

	
}
