package com.boeing.cas.supa.ground.pojos;

public class AzureStorageMessage {

	private String containerName;
	private String fileName;
	private UserCondensed uploadedBy;
	private long uploadedOn;

	public AzureStorageMessage() {}

	public AzureStorageMessage(String containerName, String fileName, UserCondensed uploadedBy, long uploadedOn) {
		this.containerName = containerName;
		this.fileName = fileName;
		this.uploadedBy = uploadedBy;
		this.uploadedOn = uploadedOn;
	}

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

		return new StringBuilder("[").append(this.getClass().getSimpleName()).append(']').append(':')
				.append("containerName=").append(containerName).append(',')
				.append("fileName=").append(fileName)
				.toString();
	}
}
