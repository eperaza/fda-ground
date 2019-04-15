package com.boeing.cas.supa.ground.pojos;


public class UserAccount extends User {

	protected String userRole;

	public UserAccount() {
		// User Account created
	}

	public UserAccount(String userRole) {
		this.userRole = userRole;
	}

	public UserAccount (User user) {
		System.out.println("Update User Account for: " + user.getObjectId());
		this.setObjectId(user.getObjectId());
		this.setObjectType(user.getObjectType());
		this.setDisplayName(user.getDisplayName());

		this.setUserPrincipalName(user.getUserPrincipalName());
		this.setAccountEnabled(user.getAccountEnabled());
		this.setCity(user.getCity());
		this.setCountry(user.getCountry());
		this.setDepartment(user.getDepartment());
		this.setDirSyncEnabled(user.getDirSyncEnabled());
		this.setFacsimileTelephoneNumber(user.getFacsimileTelephoneNumber());
		this.setGivenName(user.getGivenName());
		this.setJobTitle(user.getJobTitle());
		this.setLastDirSyncTime(user.getLastDirSyncTime());
		this.setMail(user.getMail());
		this.setMailNickname(user.getMailNickname());
		this.setMobile(user.getMobile());
		this.setPassword(user.getPassword());
		this.setPasswordPolicies(user.getPasswordPolicies());
		this.setPhysicalDeliveryOfficeName(user.getPhysicalDeliveryOfficeName());
		this.setPostalCode(user.getPostalCode());
		this.setPreferredLanguage(user.getPreferredLanguage());
		this.setState(user.getState());
		this.setStreetAddress(user.getStreetAddress());
		this.setSurname(user.getSurname());
		this.setTelephoneNumber(user.getTelephoneNumber());
		this.setUsageLocation(user.getUsageLocation());
		this.setUserPrincipalName(user.getUserPrincipalName());
		this.setCreatedDateTime(user.getCreatedDateTime());

		if (user.getOtherMails() != null && !user.getOtherMails().isEmpty()) {
			this.setOtherMails(user.getOtherMails());
		}

		if (user.getGroups() != null && !user.getGroups().isEmpty()) {
			this.setGroups(user.getGroups());
		}

		if (user.getRoles() != null && !user.getRoles().isEmpty()) {
			this.setRoles(user.getRoles());
		}

		if (user.getDirectoryRoles() != null && !user.getDirectoryRoles().isEmpty()) {
			this.setDirectoryRoles(user.getDirectoryRoles());
		}

	}

	public String getUserRole() {
		return userRole;
	}

	public void setUserRole(String userRole) {
		this.userRole = userRole;
	}

}
