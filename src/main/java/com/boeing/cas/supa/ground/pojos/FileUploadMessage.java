package com.boeing.cas.supa.ground.pojos;

public class FileUploadMessage {
	private String AdwTransfer;
	private String AzureUpload;
	private String EmailSent;
	private String Message;
	
	public FileUploadMessage(String adwTransfer, String azureUpload, String emailSent, String message) {
		super();
		AdwTransfer = adwTransfer;
		AzureUpload = azureUpload;
		EmailSent = emailSent;
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
	public String getEmailSent() {
		return EmailSent;
	}
	public void setEmailSent(String emailSent) {
		EmailSent = emailSent;
	}
	public String getMessage() {
		return Message;
	}
	public void setMessage(String message) {
		Message = message;
	}
}
