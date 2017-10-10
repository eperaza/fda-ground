package com.boeing.cas.supa.ground.utils;

public class AzureStorageMessage {
	String containerName;
	String fileName;

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

	@Override
	public String toString() {
		return "StorageMessage [containerName=" + containerName + ", fileName=" + fileName + "]";
	}
}
