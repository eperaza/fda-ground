package com.boeing.cas.supa.ground.utils;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "azurekeyvault")
public class KeyVaultProperties {
	private String clientKey;

    private String clientId;
    
    private String uri;

	public String getClientKey() {
		return clientKey;
	}

	public void setClientKey(String clientKey) {
		this.clientKey = clientKey;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
    
    
    
    
}
