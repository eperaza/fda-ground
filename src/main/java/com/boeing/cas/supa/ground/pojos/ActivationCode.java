package com.boeing.cas.supa.ground.pojos;


public class ActivationCode {

	private String appIDName;
	private String certificate;

	public String getAppIDName() {
		return appIDName;
	}

	public void setAppIDName(String appIDName) {
		this.appIDName = appIDName;
	}

	public String getCertificate() {
		return certificate;
	}

	public void setCertificate(String certificate) {
		this.certificate = certificate;
	}

	@Override
	public String toString() {
		return "ActivationCode{" +
				"appIDName='" + appIDName + '\'' +
				", certificate='" + certificate + '\'' +
				'}';
	}
}

