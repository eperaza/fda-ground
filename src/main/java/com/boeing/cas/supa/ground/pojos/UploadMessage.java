package com.boeing.cas.supa.ground.pojos;

public class UploadMessage {
	private String AdwTransfer;
	private String AzureUpload;
	private String Message;
	
	public UploadMessage(String adwTransfer, String azureUpload, String message) {
		super();
		AdwTransfer = adwTransfer;
		AzureUpload = azureUpload;
		Message = message;
	}
	public String getAdwTransfer() {
		return AdwTransfer;
	}
	public void setAdwTransfer(String adwTransfer) {
		AdwTransfer = adwTransfer;
	}
	public String getAzureUpload() {
		return AzureUpload;
	}
	public void setAzureUpload(String azureUpload) {
		AzureUpload = azureUpload;
	}

	public String getMessage() {
		return Message;
	}
	public void setMessage(String message) {
		Message = message;
	}
}
