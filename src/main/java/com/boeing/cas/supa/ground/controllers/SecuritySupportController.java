package com.boeing.cas.supa.ground.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.boeing.cas.supa.ground.exceptions.ApiErrorException;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.Group;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.services.AzureADClientService;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.KeyVaultRetriever;

@Controller
public class SecuritySupportController {

    @Autowired
    private KeyVaultRetriever keyVaultRetriever;

    @Autowired
    private AzureADClientService azureADClientService;

    @RequestMapping(path = "/getZuppaPassword", method = {RequestMethod.GET})
    public ResponseEntity<String> getZuppaPassword(@RequestHeader("Authorization") String authToken) throws ApiErrorException {
        final User user = azureADClientService.getUserInfoFromJwtAccessToken(authToken);
        List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
        if (airlineGroups.size() != 1) {
            throw new ApiErrorException("GET_ZUPPA_PASSWORD_FAILURE", new ApiError("GET_ZUPPA_PASSWORD_FAILURE", "Failed to associate user with an airline", Constants.RequestFailureReason.UNAUTHORIZED));
        }
        String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);

        String zuppaPassword = keyVaultRetriever.getSecretByKey(String.format("%s%s", Constants.ZUPPA_SECRET_PREFIX, airlineGroup.toLowerCase()));

        return new ResponseEntity<>(zuppaPassword, HttpStatus.OK);
    }
}