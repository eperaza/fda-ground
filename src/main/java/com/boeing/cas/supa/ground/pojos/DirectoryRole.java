package com.boeing.cas.supa.ground.pojos;

public class DirectoryRole extends DirectoryObject {

	protected String description;
    protected boolean isSystem;
    protected boolean roleDisabled;

    private DirectoryRole() {}

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isIsSystem() {
		return isSystem;
	}

	public void setIsSystem(boolean isSystem) {
		this.isSystem = isSystem;
	}

	public boolean isRoleDisabled() {
		return roleDisabled;
	}

	public void setRoleDisabled(boolean roleDisabled) {
		this.roleDisabled = roleDisabled;
	}

	public String toString() {

		return new StringBuilder('[').append(this.getClass().getSimpleName()).append(']').append(':')
        		.append("objectId=").append(this.objectId).append(',')
        		.append("objectType=").append(this.objectType).append(',')
        		.append("displayName=").append(this.displayName).append(',')
        		.append("description=").append(this.description).append(',')
        		.append("isSystem=").append(this.isSystem).append(',')
        		.append("roleDisabled=").append(this.roleDisabled)
        		.toString();
    }
}
