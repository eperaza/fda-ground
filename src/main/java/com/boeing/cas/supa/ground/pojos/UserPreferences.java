package com.boeing.cas.supa.ground.pojos;


public class UserPreferences {

	protected String airline;
	protected String preference;
	protected String userKey;
	protected boolean enabled;
	protected String description;
	protected String groupBy;
	protected boolean toggle;
	protected String value;
	protected String updatedBy;
	protected String createdDateTime;

	public UserPreferences() {
		// User Account created
	}

	public UserPreferences(String airline) {
		this.airline = airline;
	}

	public String getAirline() {
		return airline;
	}

	public void setAirline(String airline) {
		this.airline = airline;
	}

	public String getPreference() {
		return preference;
	}

	public void setPreference(String preference) {
		this.preference = preference;
	}

	public String getUserKey() {
		return userKey;
	}

	public void setUserKey(String userKey) {
		this.userKey = userKey;
	}

	public boolean isEnabled() { return enabled; }

	public void setEnabled(boolean enabled) { this.enabled = enabled; }

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getGroupBy() {
		return groupBy;
	}

	public void setGroupBy(String groupBy) {
		this.groupBy = groupBy;
	}

	public boolean isToggle() { return toggle; }

	public void setToggle(boolean toggle) { this.toggle = toggle; }

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public String getCreatedDateTime() {
		return createdDateTime;
	}

	public void setCreatedDateTime(String createdDateTime) {
		this.createdDateTime = createdDateTime;
	}

	@Override
	public String toString() {
		return "UserPreferences{" +
				"airline='" + airline + '\'' +
				", preference='" + preference + '\'' +
				", userKey='" + userKey + '\'' +
				", enabled='" + enabled + '\'' +
				", description='" + description + '\'' +
				", groupBy='" + groupBy + '\'' +
				", toggle='" + toggle + '\'' +
				", value='" + value + '\'' +
				", updatedBy='" + updatedBy + '\'' +
				", createdDateTime='" + createdDateTime + '\'' +
				'}';
	}
}
