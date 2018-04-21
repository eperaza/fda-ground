package com.boeing.cas.supa.ground.pojos;

public class AzureStorageMessage {

	protected String containerName;
	protected String fileName;
	protected UserCondensed uploadedBy;
	protected long uploadedOn;

	public String getContainerName() {
		return this.containerName;
	}

	public void setContainerName(String containerName) {
		this.containerName = containerName;
	}

	public String getFileName() {
		return this.fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public UserCondensed getUploadedBy() {
		return this.uploadedBy;
	}

	public long getUploadedOn() {
		return this.uploadedOn;
	}

	public void setUploadedBy(UserCondensed uploadedBy) {
		this.uploadedBy = uploadedBy;
	}

	public void setUploadedOn(long uploadedOn) {
		this.uploadedOn = uploadedOn;
	}

	@Override
	public String toString() {

		return new StringBuilder("AzureStorageMessage [")
				.append("containerName=").append(containerName).append(',')
				.append("fileName=").append(fileName)
				.append(']')
				.toString();
	}
}
