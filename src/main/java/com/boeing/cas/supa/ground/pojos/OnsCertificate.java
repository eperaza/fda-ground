package com.boeing.cas.supa.ground.pojos;

import java.util.Arrays;

public class OnsCertificate {

	private String key = "";
	private byte[] certificate = new byte[0];

	public OnsCertificate(String key, byte[] certificate) {

		this.key = key;
		this.certificate = certificate;

	}

	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
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
				"key='" + this.key + '\'' +
				", certificate=" + Arrays.toString(this.certificate) +
				'}';
	}
}
