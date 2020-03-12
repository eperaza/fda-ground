package com.boeing.cas.supa.ground.pojos;


public class AirlinePreferences {

	protected String airline;
	protected String preference;
	protected String airlineKey;
	protected boolean enabled;
	protected String description;
	protected boolean choicePilot;
	protected boolean choiceFocal;
	protected boolean choiceCheckAirman;
	protected boolean choiceMaintenance;
	protected boolean choiceEfbAdmin;

	protected String updatedBy;
	protected String createdDateTime;

	public AirlinePreferences() {
		// User Account created
	}

	public AirlinePreferences(String airline) {
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

	public String getAirlineKey() {
		return airlineKey;
	}

	public void setAirlineKey(String airlineKey) {
		this.airlineKey = airlineKey;
	}

	public boolean isEnabled() { return enabled; }

	public void setEnabled(boolean enabled) { this.enabled = enabled; }

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isChoicePilot() {
		return choicePilot;
	}

	public void setChoicePilot(boolean bChoicePilot) {
		this.choicePilot = bChoicePilot;
	}

	public boolean isChoiceFocal() {
		return choiceFocal;
	}

	public void setChoiceFocal(boolean bChoiceFocal) {
		this.choiceFocal = bChoiceFocal;
	}

	public boolean isChoiceCheckAirman() {
		return choiceCheckAirman;
	}

	public void setChoiceCheckAirman(boolean bChoiceCheckAirman) {
		this.choiceCheckAirman = bChoiceCheckAirman;
	}

	public boolean isChoiceMaintenance() {
		return choiceMaintenance;
	}

	public void setChoiceMaintenance(boolean bChoiceMaintenance) {
		this.choiceMaintenance = bChoiceMaintenance;
	}

	public boolean isChoiceEfbAdmin() {
		return choiceEfbAdmin;
	}

	public void setChoiceEfbAdmin(boolean bChoiceEfbAdmin) {
		this.choiceEfbAdmin = bChoiceEfbAdmin;
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
		return "AirlinePreferences{" +
				"airline='" + airline + '\'' +
				", preference='" + preference + '\'' +
				", airlineKey='" + airlineKey + '\'' +
				", enabled='" + enabled + '\'' +
				", description='" + description + '\'' +
				", choicePilot=" + choicePilot +
				", choiceFocal=" + choiceFocal +
				", choiceCheckAirman=" + choiceCheckAirman +
				", choiceMaintenance=" + choiceMaintenance +
				", updatedBy='" + updatedBy + '\'' +
				", createdDateTime='" + createdDateTime + '\'' +
				'}';
	}
}
