package com.boeing.cas.supa.ground.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import com.microsoft.rest.credentials.ServiceClientCredentials;

public class KeyVaultAuthenticator {
	
	public static KeyVaultClient getAuthenticatedClient(String clientId, String clientKey) {
		//Creates the KeyVaultClient using the created credentials.
		try {
			
			return new KeyVaultClient(createCredentials(clientId, clientKey));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Creates a new KeyVaultCredential based on the access token obtained.
	 * @param clientKey 
	 * @param clientId 
	 * @return
	 */
	private static ServiceClientCredentials createCredentials(String clientId, String clientKey) throws Exception {
		return new KeyVaultCredentials() {

			@Override
			public String doAuthenticate(String authorization, String resource, String scope) {
				try {
					AuthenticationResult authResult = getAccessToken(authorization, resource);
					return authResult.getAccessToken();

				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}

			private AuthenticationResult getAccessToken(String authorization, String resource) throws Exception {
				 
				AuthenticationResult result = null;
				ExecutorService service = null;
				try {
					service = Executors.newFixedThreadPool(1);
					AuthenticationContext context = new AuthenticationContext(authorization, false, service);

					Future<AuthenticationResult> future = null;

					if (clientKey != null && clientId != null) {
						ClientCredential credentials = new ClientCredential(clientId, clientKey);
						future = context.acquireToken(resource, credentials, null);
					}


					result = future.get();
				} finally {
					service.shutdown();
				}

				if (result == null) {
					throw new RuntimeException("authentication result was null");
				}
				return result;
			}

		};
	}


}
