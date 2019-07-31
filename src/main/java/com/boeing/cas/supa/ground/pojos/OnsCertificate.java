package com.boeing.cas.supa.ground.pojos;

import java.util.Arrays;

public class OnsCertificate {

	private String password = "";
	private byte[] certificate = new byte[0];

	public OnsCertificate(String password, byte[] certificate) {

		this.password = password;
		this.certificate = certificate;

	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public byte[] getCertificate() {
		return certificate;
	}

	public void setCertificate(byte[] certificate) {
		this.certificate = certificate;
	}

	@Override
	public String toString() {
		return "OnsCertificate{" +
				"password='" + password + '\'' +
				", certificate=" + Arrays.toString(certificate) +
				'}';
	}
}
