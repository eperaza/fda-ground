package com.boeing.cas.supa.ground.utils;

import com.boeing.cas.supa.ground.pojos.UserCondensed;

public class AzureStorageMessage {
	protected String containerName;
	protected String fileName;
	protected UserCondensed uploadedBy;
	protected long uploadedOn;


	public String getContainerName() {
		return containerName;
	}

	public void setContainerName(String containerName) {
		this.containerName = containerName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public UserCondensed getUploadedBy() {
		return uploadedBy;
	}

	public long getUploadedOn() {
		return uploadedOn;
	}

	public void setUploadedBy(UserCondensed uploadedBy) {
		this.uploadedBy = uploadedBy;
	}

	public void setUploadedOn(long uploadedOn) {
		this.uploadedOn = uploadedOn;
	}

	@Override
	public String toString() {
		return "StorageMessage [containerName=" + containerName + ", fileName=" + fileName + "]";
	}
}
