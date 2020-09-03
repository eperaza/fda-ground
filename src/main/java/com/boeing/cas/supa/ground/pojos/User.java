package com.boeing.cas.supa.ground.pojos;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class User extends DirectoryObject {

	protected String accountEnabled;
	protected String city;
	protected String country;
	protected String department;
	protected String dirSyncEnabled;
	protected String facsimileTelephoneNumber;
	protected String givenName;
	protected String jobTitle;
	protected String lastDirSyncTime;
	protected String mail;
	protected String mailNickname;
	protected String mobile;
	protected List<String> otherMails;
	protected String password;
	protected String passwordPolicies;
	protected String physicalDeliveryOfficeName;
	protected String postalCode;
	protected String preferredLanguage;
	protected String state;
	protected String streetAddress;
	protected String surname;
	protected String telephoneNumber;
	protected String usageLocation;
	protected String userPrincipalName;

	protected String createdDateTime;

	// The groups holds a list of group entity(s) this user belongs to.
	private List<Group> groups = new ArrayList<>();

	// The roles holds a list of role entity(s) this user belongs to.
	private List<Group> roles = new ArrayList<>();
	
	// The directory roles this user is assigned with.
	private List<DirectoryRole> directoryRoles = new ArrayList<>();

	public String getObjectId() {
		return this.objectId;
	}

	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	public String getObjectType() {
		return this.objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public String getUserPrincipalName() {
		return this.userPrincipalName;
	}

	public void setUserPrincipalName(String userPrincipalName) {
		this.userPrincipalName = userPrincipalName;
	}

	public String getUsageLocation() {
		return this.usageLocation;
	}

	public void setUsageLocation(String usageLocation) {
		this.usageLocation = usageLocation;
	}

	public String getTelephoneNumber() {
		return this.telephoneNumber;
	}

	public void setTelephoneNumber(String telephoneNumber) {
		this.telephoneNumber = telephoneNumber;
	}

	public String getSurname() {
		return this.surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getStreetAddress() {
		return this.streetAddress;
	}

	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
	}

	public String getState() {
		return this.state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getPreferredLanguage() {
		return this.preferredLanguage;
	}

	public void setPreferredLanguage(String preferredLanguage) {
		this.preferredLanguage = preferredLanguage;
	}

	public String getPostalCode() {
		return this.postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getPhysicalDeliveryOfficeName() {
		return this.physicalDeliveryOfficeName;
	}

	public void setPhysicalDeliveryOfficeName(String physicalDeliveryOfficeName) {
		this.physicalDeliveryOfficeName = physicalDeliveryOfficeName;
	}

	public String getPasswordPolicies() {
		return this.passwordPolicies;
	}

	public void setPasswordPolicies(String passwordPolicies) {
		this.passwordPolicies = passwordPolicies;
	}

	public String getMobile() {
		return this.mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getMail() {
		return this.mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getMailNickname() {
		return this.mailNickname;
	}

	public void setMailNickname(String mailNickname) {
		this.mailNickname = mailNickname;
	}

	public String getJobTitle() {
		return this.jobTitle;
	}

	public void setJobTitle(String jobTitle) {
		this.jobTitle = jobTitle;
	}

	public String getGivenName() {
		return this.givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	public String getFacsimileTelephoneNumber() {
		return this.facsimileTelephoneNumber;
	}

	public void setFacsimileTelephoneNumber(String facsimileTelephoneNumber) {
		this.facsimileTelephoneNumber = facsimileTelephoneNumber;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDirSyncEnabled() {
		return this.dirSyncEnabled;
	}

	public void setDirSyncEnabled(String dirSyncEnabled) {
		this.dirSyncEnabled = dirSyncEnabled;
	}

	public String getDepartment() {
		return this.department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getLastDirSyncTime() {
		return this.lastDirSyncTime;
	}

	public void setLastDirSyncTime(String lastDirSyncTime) {
		this.lastDirSyncTime = lastDirSyncTime;
	}

	public String getCountry() {
		return this.country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCity() {
		return this.city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getAccountEnabled() {
		return this.accountEnabled;
	}

	public void setAccountEnabled(String accountEnabled) {
		this.accountEnabled = accountEnabled;
	}

	public List<Group> getGroups() {
		return this.groups;
	}

	public void setGroups(List<Group> group) {
		this.groups = group;
	}

	public List<Group> getRoles() {
		return this.roles;
	}

	public void setRoles(List<Group> roles) {
		this.roles = roles;
	}

	public List<DirectoryRole> getDirectoryRoles() {
		return this.directoryRoles;
	}

	public void setDirectoryRoles(List<DirectoryRole> directoryRoles) {
		this.directoryRoles = directoryRoles;
	}

	public List<String> getOtherMails() {
		return this.otherMails;
	}

	public void setOtherMails(List<String> emails) { this.otherMails = emails; }

	public String getCreatedDateTime() { return this.createdDateTime;	}

	public void setCreatedDateTime(String createdDateTime) { this.createdDateTime = createdDateTime; }

    @Override
	public String toString() {

		return new StringBuilder("[").append(this.getClass().getSimpleName()).append(']').append(':')
				.append("objectId=").append(this.objectId).append(',')
				.append("objectType=").append(this.objectType).append(',')
				.append("displayName=").append(this.displayName).append(',')
				.append("accountEnabled=").append(this.accountEnabled).append(',')
				.append("city=").append(this.city).append(',')
				.append("country=").append(this.country).append(',')
				.append("department=").append(this.department).append(',')
				.append("dirSyncEnabled=").append(this.dirSyncEnabled).append(',')
				.append("facsimileTelephoneNumber=").append(this.facsimileTelephoneNumber).append(',')
				.append("givenName=").append(this.givenName).append(',')
				.append("jobTitle=").append(this.jobTitle).append(',')
				.append("lastDirSyncTime=").append(this.lastDirSyncTime).append(',')
				.append("mail=").append(this.mail).append(',')
				.append("mailNickname=").append(this.mailNickname).append(',')
				.append("mobile=").append(this.mobile).append(',')
				.append("password=").append(this.password).append(',')
				.append("passwordPolicies=").append(this.passwordPolicies).append(',')
				.append("physicalDeliveryOfficeName=").append(this.physicalDeliveryOfficeName).append(',')
				.append("postalCode=").append(this.postalCode).append(',')
				.append("preferredLanguage=").append(this.preferredLanguage).append(',')
				.append("state=").append(this.state).append(',')
				.append("streetAddress=").append(this.streetAddress).append(',')
				.append("surname=").append(this.surname).append(',')
				.append("telephoneNumber=").append(this.telephoneNumber).append(',')
				.append("usageLocation=").append(this.usageLocation).append(',')
				.append("userPrincipalName=").append(this.userPrincipalName).append(',')
				.append("createdDateTime=").append(this.createdDateTime).append(',')
				.append("otherMails=").append(StringUtils.join(this.otherMails, ","))
			.toString();
	}
}
