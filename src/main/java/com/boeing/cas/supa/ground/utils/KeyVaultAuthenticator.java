package com.boeing.cas.supa.ground.utils;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import com.microsoft.rest.credentials.ServiceClientCredentials;

public class KeyVaultAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(KeyVaultAuthenticator.class);

    // Hide default constructor
    private KeyVaultAuthenticator() {}

    public static KeyVaultClient getAuthenticatedClient(String clientId, String clientKey) {

    	// Creates the KeyVaultClient using the created credentials.
		try {
			return new KeyVaultClient(KeyVaultAuthenticator.createCredentials(clientId, clientKey));
		} catch (Exception e) {
            logger.error("Failed to instantiate keyvault client: {}", e.getMessage(), e);
		}

		return null;
	}

	private static ServiceClientCredentials createCredentials(final String clientId, String clientKey) throws MalformedURLException, ExecutionException, InterruptedException {

		return new KeyVaultCredentials() {

			@Override
			public String doAuthenticate(String authorization, String resource, String scope) {
				try {
					AuthenticationResult authResult = this.getAccessToken(authorization, resource);
					return authResult.getAccessToken();

				} catch (Exception e) {
					throw new SecurityException(e);
				}
			}

			private AuthenticationResult getAccessToken(String authorization, String resource) throws MalformedURLException, ExecutionException, InterruptedException {

				AuthenticationResult result = null;
				ExecutorService service = null;
				try {

					service = Executors.newFixedThreadPool(1);
					AuthenticationContext context = new AuthenticationContext(authorization, false, service);

					if (clientKey != null && clientId != null) {

						ClientCredential credentials = new ClientCredential(clientId, clientKey);
						Future<AuthenticationResult> future = context.acquireToken(resource, credentials, null);
						result = (AuthenticationResult) future.get();
					}
				} finally {
					if (service != null) { service.shutdown(); }
				}

				if (result == null) {
					throw new SecurityException("authentication result was null");
				}

				return result;
			}
		};
	}
}
