package com.boeing.cas.supa.ground.pojos;

public class UploadMessage {

	private String adwTransfer;
	private String azureUpload;
	private String message;

	public UploadMessage(String adwTransfer, String azureUpload, String message) {

		this.adwTransfer = adwTransfer;
		this.azureUpload = azureUpload;
		this.message = message;
	}

	public String getAdwTransfer() {
		return this.adwTransfer;
	}

	public void setAdwTransfer(String adwTransfer) {
		this.adwTransfer = adwTransfer;
	}

	public String getAzureUpload() {
		return this.azureUpload;
	}

	public void setAzureUpload(String azureUpload) {
		this.azureUpload = azureUpload;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
