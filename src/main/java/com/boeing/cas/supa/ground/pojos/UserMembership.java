package com.boeing.cas.supa.ground.pojos;

import java.util.List;

public class UserMembership {

	private List<Group> userGroups;
	private List<DirectoryRole> userRoles;

	public UserMembership(List<Group> userGroupMembership, List<DirectoryRole> userRoleMembership) {
		this.userGroups = userGroupMembership;
		this.userRoles = userRoleMembership;
	}

	public List<Group> getUserGroups() {
		return userGroups;
	}

	public void setUserGroups(List<Group> userGroups) {
		this.userGroups = userGroups;
	}

	public List<DirectoryRole> getUserRoles() {
		return userRoles;
	}

	public void setUserRoles(List<DirectoryRole> userRoles) {
		this.userRoles = userRoles;
	}
}
