package com.boeing.cas.supa.ground.pojos;

import java.util.ArrayList;
import java.util.List;

public class UserCondensed {

	protected String surname;
	protected String givenName;
	protected String displayName;
	protected List<String> otherMails;

	protected List<Group> groups = new ArrayList<>();

	public UserCondensed(String surname, String givenName, String displayName, List<String> otherMails, List<Group> groups) {

		this.surname = surname;
		this.givenName = givenName;
		this.displayName = displayName;
		this.otherMails = otherMails;
		this.groups = groups;
	}

	public String getSurname() {
		return this.surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getGivenName() {
		return this.givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public List<String> getOtherMails() {
		return this.otherMails;
	}

	public void setOtherMails(List<String> otherMails) {
		this.otherMails = otherMails;
	}

	public List<Group> getGroups() {
		return this.groups;
	}

	public void setGroups(List<Group> groups) {
		this.groups = groups;
	}
}
