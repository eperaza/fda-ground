package com.boeing.cas.supa.ground.helpers;

import javax.servlet.http.HttpServletRequest;

import com.boeing.cas.supa.ground.pojos.AuthParameterNames;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;

public final class AzureADAuthHelper {

    public static final String PRINCIPAL_SESSION_NAME = "principal";

    private AzureADAuthHelper() {
    }

    public static boolean isAuthenticated(HttpServletRequest request) {
        return request.getSession().getAttribute(PRINCIPAL_SESSION_NAME) != null;
    }

    public static AuthenticationResult getAuthSessionObject(HttpServletRequest request) {
        return (AuthenticationResult) request.getSession().getAttribute(PRINCIPAL_SESSION_NAME);
    }

    public static boolean containsAuthenticationData(HttpServletRequest httpRequest) {
        return httpRequest.getMethod().equalsIgnoreCase("POST")
        		&& (httpRequest.getParameterMap().containsKey(AuthParameterNames.ERROR)
                    || httpRequest.getParameterMap().containsKey(AuthParameterNames.ID_TOKEN)
                    || httpRequest.getParameterMap().containsKey(AuthParameterNames.CODE));
    }

    public static boolean isAuthenticationSuccessful(AuthenticationResponse authResponse) {
        return authResponse instanceof AuthenticationSuccessResponse;
    }
}
