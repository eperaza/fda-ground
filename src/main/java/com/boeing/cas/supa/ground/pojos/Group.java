package com.boeing.cas.supa.ground.pojos;

public class Group extends DirectoryObject {

	protected String description;
    protected String mail;

    private Group() {}

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMail() {
        return this.mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String toString() {

		return new StringBuilder('[').append(this.getClass().getSimpleName()).append(']').append(':')
        		.append("objectId=").append(this.objectId).append(',')
        		.append("objectType=").append(this.objectType).append(',')
        		.append("displayName=").append(this.displayName).append(',')
        		.append("description=").append(this.description).append(',')
        		.append("mail=").append(this.mail)
        		.toString();
    }
}
