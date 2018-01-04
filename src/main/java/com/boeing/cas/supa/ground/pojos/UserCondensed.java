package com.boeing.cas.supa.ground.pojos;

import java.util.ArrayList;

public class UserCondensed {
	protected String surname;
	protected String givenName;
	protected String displayName;
	protected ArrayList<String> otherMails;
	protected ArrayList<Group> groups;
	public UserCondensed(String surname, String givenName, String displayName, ArrayList<String> otherMails, ArrayList<Group> groups) {
		this.surname = surname;
		this.givenName = givenName;
		this.displayName = displayName;
		this.otherMails = otherMails;
		this.groups = groups;
	}
	public String getSurname() {
		return surname;
	}
	public void setSurname(String surname) {
		this.surname = surname;
	}
	public String getGivenName() {
		return givenName;
	}
	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public ArrayList<String> getOtherMails() {
		return otherMails;
	}
	public void setOtherMails(ArrayList<String> otherMails) {
		this.otherMails = otherMails;
	}
	public ArrayList<Group> getGroups() {
		return groups;
	}
	public void setGroups(ArrayList<Group> groups) {
		this.groups = groups;
	}
	

}
