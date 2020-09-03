package com.boeing.cas.supa.ground.pojos;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class NewUser {

	private String userPrincipalName;
	private boolean accountEnabled;
	private String givenName;
	private String surname;
	private String displayName;
	private String password;
	private boolean forceChangePasswordNextLogin;
	public String airlineGroupName;
	private String roleGroupName;
	private List<String> otherMails;

	public NewUser(){
		this.otherMails = new ArrayList<String>();
	}

	public NewUser(String userPrincipalName, String givenName, String surname, String password, String email, Group airlineGroup, String roleGroupName){
		this.otherMails = new ArrayList<String>();
		this.userPrincipalName = userPrincipalName;
		this.givenName = givenName;
		this.displayName = userPrincipalName;
		this.accountEnabled = true;
		this.forceChangePasswordNextLogin = false;
		this.surname = surname;
		this.password = password;
		this.otherMails.add(email);
		this.airlineGroupName = airlineGroup.description;
		this.roleGroupName = roleGroupName;
	}

	public String getUserPrincipalName() {
		return userPrincipalName;
	}

	public void setUserPrincipalName(String userPrincipalName) {
		this.userPrincipalName = userPrincipalName;
	}

	public boolean isAccountEnabled() {
		return accountEnabled;
	}

	public void setAccountEnabled(boolean accountEnabled) {
		this.accountEnabled = accountEnabled;
	}

	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isForceChangePasswordNextLogin() {
		return forceChangePasswordNextLogin;
	}

	public void setForceChangePasswordNextLogin(boolean forceChangePasswordNextLogin) {
		this.forceChangePasswordNextLogin = forceChangePasswordNextLogin;
	}

	public String getAirlineGroupName() {
		return airlineGroupName;
	}

	public void setAirlineGroupName(String airlineGroupName) {
		this.airlineGroupName = airlineGroupName;
	}

	public String getRoleGroupName() {
		return roleGroupName;
	}

	public void setRoleGroupName(String roleGroupName) {
		this.roleGroupName = roleGroupName;
	}

	public List<String> getOtherMails() {
		return otherMails;
	}

	public void setOtherMails(List<String> otherMails) {
		this.otherMails = otherMails;
	}

	@Override
	public String toString() {

		return new StringBuilder("[").append(this.getClass().getSimpleName()).append(']').append(':')
				.append("userPrincipalName=").append(this.userPrincipalName).append(',')
				.append("accountEnabled=").append(this.accountEnabled).append(',')
				.append("givenName=").append(this.givenName).append(',')
				.append("surname=").append(this.surname).append(',')
				.append("displayName=").append(this.displayName).append(',')
				.append("password=").append(this.password).append(',')
				.append("forceChangePasswordNextLogin=").append(this.forceChangePasswordNextLogin).append(',')
				.append("airlineGroupName=").append(this.airlineGroupName).append(',')
				.append("roleGroupName=").append(this.roleGroupName).append(',')
				.append("otherMails=").append(StringUtils.join(this.otherMails, ",")).
			toString();
	}
}
