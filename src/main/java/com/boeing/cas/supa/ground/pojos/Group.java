package com.boeing.cas.supa.ground.pojos;

public class Group extends DirectoryObject {
	protected String objectId;
	protected String objectType;
	protected String displayName;
	protected String description;
    protected String mail;

    private Group() {
    }

    @Override
    public String getObjectId() {
        return objectId;
    }

    @Override
    public String getObjectType() {
        return objectType;
    }

    @Override
    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    @Override
    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    

}
