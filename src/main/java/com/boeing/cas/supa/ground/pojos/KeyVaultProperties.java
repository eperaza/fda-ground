package com.boeing.cas.supa.ground.pojos;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "azurekeyvault")
public class KeyVaultProperties {

	private String clientKey;
	private String clientId;
	private String uri;
	private String zuppaUri;

	public String getClientKey() {
		return this.clientKey;
	}

	public void setClientKey(String clientKey) {
		this.clientKey = clientKey;
	}

	public String getClientId() {
		return this.clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getUri() {
		return this.uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getZuppaUri() {
		return this.zuppaUri;
	}

	public void setZuppaUri(String zuppaUri) {
		this.zuppaUri = zuppaUri;
	}
}
