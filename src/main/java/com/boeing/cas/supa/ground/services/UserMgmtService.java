package com.boeing.cas.supa.ground.services;

import com.boeing.cas.supa.ground.dao.FeatureManagementDao;
import com.boeing.cas.supa.ground.dao.PowerBiInformationDao;
import com.boeing.cas.supa.ground.dao.UserAccountPreregistrationDao;
import com.boeing.cas.supa.ground.dao.UserAccountRegistrationDao;
import com.boeing.cas.supa.ground.exceptions.*;
import com.boeing.cas.supa.ground.helpers.AzureADClientHelper;
import com.boeing.cas.supa.ground.helpers.HttpClientHelper;
import com.boeing.cas.supa.ground.pojos.*;
import com.boeing.cas.supa.ground.utils.*;
import com.boeing.cas.supa.ground.utils.Constants.PermissionType;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.boeing.cas.supa.ground.utils.Constants;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.azure.storage.StorageException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.mail.internet.MimeMessage;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.boeing.cas.supa.ground.services.FileManagementService.TSP_CONFIG_ZIP_CONTAINER;

@Service
public class UserMgmtService {

    private final Logger logger = LoggerFactory.getLogger(UserMgmtService.class);

    // include any path which uses actuator endpoints
    private static final Set<String> ALLOWED_ROLES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList("role-airlinefocal", "role-airlineefbadmin", "role-airlinesuperadmin")));

    @Value("${api.azuread.version}")
    private String azureadApiVersion;

    @Value("${api.azuread.uri}")
    private String azureadApiUri;

    @Value("${api.msgraph.uri}")
    private String msgraphApiUri;

    @Autowired
    private Map<String, String> appProps;

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private UserAccountRegistrationDao userAccountRegister;

    @Autowired
    private UserAccountPreregistrationDao userAccountPreregister;

    @Autowired
    private AzureADClientService aadClient;

    @Autowired
    private FeatureManagementDao featureManagementDao;

    @Autowired
    FileManagementService fileManagementService;

    @Autowired
    private PowerBiInformationDao powerBiInformationDao;

    @Autowired
    private EmailRegistrationService emailService;

    public Object getUsers(String objectID, String airline) {

        Object resultObj = null;
        StringBuilder progressLog = new StringBuilder("Get Users -");

        try {

            // Get group membership of user issuing request. Ensure that user belongs to
            // role-airlinefocal group
            // and one and only one airline group.

            List<UserAccount> users = userAccountRegister.getAllUsers(airline, objectID);

            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setSerializationInclusion(Include.NON_NULL);

            logger.info("returned [{}] members of Airline Group", users.size());

            resultObj = mapper.setSerializationInclusion(Include.NON_NULL).setSerializationInclusion(Include.NON_EMPTY)
                    .writeValueAsString(users);
            progressLog.append("\nObtained members of group in SQL DB");

        } catch (JsonProcessingException jpe) {
            logger.error("JsonProcessingException: {}", jpe.getMessage(), jpe);
            resultObj = new ApiError("USERS_LIST_FAILED", "Failed to format user record.");
        } catch (UserAccountRegistrationException uare) {
            logger.error("UserAccountRegistrationException: {}", uare.getMessage(), uare);
            resultObj = new ApiError("USERS_LIST_FAILED", "Unable to locate records.");
        } finally {

            if (resultObj instanceof ApiError) {
                logger.error("FDAGndSvcLog> {}", ControllerUtils.sanitizeString(progressLog.toString()));
            }
        }
        return resultObj;
    }

    public Object getPreUsers(String airline) {

        Object resultObj = null;
        StringBuilder progressLog = new StringBuilder("Get Users -");

        try {

            List<PreUserAccount> users = userAccountPreregister.getAllUsers(airline);

            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setSerializationInclusion(Include.NON_NULL);

            logger.info("returned [{}] members of Airline Group", users.size());

            resultObj = mapper.setSerializationInclusion(Include.NON_NULL).setSerializationInclusion(Include.NON_EMPTY)
                    .writeValueAsString(users);
            progressLog.append("\nObtained members of group in SQL DB");

        } catch (JsonProcessingException jpe) {
            logger.error("JsonProcessingException: {}", jpe.getMessage(), jpe);
            resultObj = new ApiError("USERS_LIST_FAILED", "Failed to format user record.");
        } catch (UserAccountRegistrationException uare) {
            logger.error("UserAccountRegistrationException: {}", uare.getMessage(), uare);
            resultObj = new ApiError("USERS_LIST_FAILED", "Unable to locate records.");
        } finally {

            if (resultObj instanceof ApiError) {
                logger.error("FDAGndSvcLog> {}", ControllerUtils.sanitizeString(progressLog.toString()));
            }
        }
        return resultObj;
    }

    public int createUser(PreUserAccount newUserPayload) throws UserAccountRegistrationException {
        // Register new user in the account registration database
        String registrationToken = UUID.randomUUID().toString();
        newUserPayload.setRegistrationToken(registrationToken);

        int returnVal = userAccountPreregister.registerNewUserAccount(newUserPayload);
        if (returnVal == 1) {
            logger.info("Registered new pre user {} in database");
        } else {
            logger.error("failure to create");
        }
        return returnVal;
    }

    public int updatePreUser(PreUserAccount newUserPayload) throws UserAccountRegistrationException {
        // Register new user in the account registration database
        int returnVal = userAccountPreregister.updateUserAccount(newUserPayload);
        if (returnVal == 1) {
            logger.info("Registered new user {} in database");
        } else {
            logger.error("failure to create");

        }
        return returnVal;
    }

    public int deletePreUser(String userId) throws UserAccountRegistrationException {
        // Delete any associated records in the User Account Registration database table
        int returnVal = userAccountPreregister.removeUserAccountRegistrationData(userId);
        logger.info("Removed account pre-registration records for user {} in database", userId);
        return returnVal;
    }

    public Object deleteUser(String userId, String objectId, boolean isSuperAdmin, String membership,
            String role) {
        
        Object resultObj = null;
        StringBuilder progressLog = new StringBuilder("Delete user -");

        try {

            // Ensure requesting user is not trying to delete self!
            if (objectId.equals(userId)) {
                return new ApiError("USER_DELETE_FAILED", "User cannot delete self", RequestFailureReason.BAD_REQUEST);
            }

            // - belong to Role-AirlineFocal or Role-EFBAdmin group and a single Airline group
            if (!ALLOWED_ROLES.contains(role) || membership.isEmpty()) {
                logger.error("Role not allowed to delete users: [{}]", role);
                return new ApiError("USER_DELETE_FAILED", "User has insufficient privileges",
                        RequestFailureReason.UNAUTHORIZED);
            }

            // Get access token based on delegated permission via impersonation with a Local
            // Administrator of the tenant.
            String adminAccessToken = null;
            Object authResult = aadClient.getElevatedPermissionsAccessToken(PermissionType.IMPERSONATION);
            if (authResult instanceof ApiError) {
                return authResult;
            }
            // The access token could not be obtained either via delegated permissions or
            // application permissions.
            else if (authResult == null) {
                return new ApiError("USER_DELETE_FAILED", "Failed to acquire permissions",
                        RequestFailureReason.UNAUTHORIZED);
            } else {
                adminAccessToken = String.valueOf(authResult);
                progressLog.append("\nObtained impersonation access token");
            }

            // Airline focal can delete a user, only if an user matching the user object ID
            // exists
            User deleteUser = aadClient.getUserInfoFromGraph(userId, adminAccessToken);
            if (deleteUser == null) {
                return new ApiError("USER_DELETE_FAILED", "No user found with matching identifier",
                        RequestFailureReason.NOT_FOUND);
            }
            UserMembership userMembership = aadClient.getUserMembershipFromGraph(deleteUser.getObjectId(),
                    adminAccessToken);
            List<Group> deleteUserGroups = userMembership.getUserGroups();
            if (deleteUserGroups != null) {

                List<Group> deleteUserAirlineGroups = deleteUserGroups.stream()
                        .filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX))
                        .collect(Collectors.toList());
                // Ensure airline focal is not deleting user in another airline, by getting
                // airline group membership
                // If operation is performed in SuperAdmin mode, skip this check.
                if (!isSuperAdmin
                        && (deleteUserAirlineGroups.size() != 1
                                || !deleteUserAirlineGroups.get(0).getObjectId().equals(membership))) {
                    return new ApiError("USER_DELETE_FAILED", "Not allowed to delete user if not in same airline group",
                            RequestFailureReason.UNAUTHORIZED);
                } else {
                    // continue
                }
            } else {
                return new ApiError("USER_DELETE_FAILED", "Not allowed to delete user if not in same airline group",
                        RequestFailureReason.UNAUTHORIZED);
            }

            progressLog.append(isSuperAdmin ? "\nUser to be deleted by a SuperAdmin"
                    : "User to be deleted is in same airline group");

            // All checks passed. The user can be deleted by invoking the Azure AD Graph
            // API.
            URL url = new URL(
                    new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
                            .append("/users/").append(userId)
                            .append('?').append(new StringBuilder(Constants.AZURE_API_VERSION_PREFIX)
                                    .append(azureadApiVersion).toString())
                            .toString());

            HttpURLConnection connDeleteUser = (HttpsURLConnection) url.openConnection();
            // Set the appropriate header fields in the request header.
            connDeleteUser.setRequestMethod(RequestMethod.DELETE.toString());
            connDeleteUser.setRequestProperty("api-version", azureadApiVersion);
            connDeleteUser.setRequestProperty("Authorization", adminAccessToken);
            connDeleteUser.setRequestProperty("Content-Type", "application/json");
            connDeleteUser.setRequestProperty("Accept", Constants.ACCEPT_CT_JSON_ODATAMINIMAL);

            String responseStr = HttpClientHelper.getResponseStringFromConn(connDeleteUser,
                    connDeleteUser.getResponseCode() == HttpStatus.NO_CONTENT.value());
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setSerializationInclusion(Include.NON_NULL);
            JsonNode errorNode = StringUtils.isBlank(responseStr) ? null
                    : mapper.readTree(responseStr).path("odata.error");
            if (connDeleteUser.getResponseCode() == HttpStatus.NO_CONTENT.value() && errorNode == null) {
                logger.info("Deleted user {} in Azure AD", deleteUser.getUserPrincipalName());
                progressLog.append("\nDeleted user in Azure AD");
            }

            // Delete any associated records in the User Account Registration database table
            userAccountRegister.removeUserAccountRegistrationData(deleteUser.getUserPrincipalName());
            logger.info("Removed account registration records for user {} in Azure AD",
                    deleteUser.getUserPrincipalName());
            progressLog.append("\nDeleted corresponding data in user account registration database");
        } catch (UserAccountRegistrationException uare) {
            logger.error("Failed to register new user account: {}", uare.getMessage());
            resultObj = new ApiError("USER_DELETE_FAILED", "FDAGNDSVCERR0001");
        } catch (JsonProcessingException jpe) {
            logger.error("JsonProcessingException: {}", jpe.getMessage(), jpe);
            resultObj = new ApiError("USER_DELETE_FAILED", "FDAGNDSVCERR0002");
        } catch (MalformedURLException murle) {
            logger.error("MalformedURLException: {}", murle.getMessage(), murle);
            resultObj = new ApiError("USER_DELETE_FAILED", "FDAGNDSVCERR0004");
        } catch (ProtocolException pe) {
            logger.error("ProtocolException: {}", pe.getMessage(), pe);
            resultObj = new ApiError("USER_DELETE_FAILED", "FDAGNDSVCERR0008");
        } catch (IOException ioe) {
            logger.error("IOException: {}", ioe.getMessage(), ioe);
            resultObj = new ApiError("USER_DELETE_FAILED", "FDAGNDSVCERR0016");
        } finally {

            if (resultObj instanceof ApiError) {
                logger.error("FDAGndSvcLog> {}", ControllerUtils.sanitizeString(progressLog.toString()));
            }
        }

        return resultObj;
    }

    public Object getAirlinePreferences(String airline) {

        Object resultObj = null;
        StringBuilder progressLog = new StringBuilder("Get Airline Preferences -");

        try {

            List<AirlinePreferences> airlinePreferences = featureManagementDao.getAirlinePreferences(airline, true);

            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL);

            logger.info("returned [{}] airline preferences records of Airline Group [{}]", airlinePreferences.size(),
                    airline);

            resultObj = mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                    .writeValueAsString(airlinePreferences);
            progressLog.append("\nObtained members of group in SQL DB");

        } catch (JsonProcessingException jpe) {
            logger.error("JsonProcessingException: {}", jpe.getMessage(), jpe);
            resultObj = new ApiError("AIRLINE_PREFERENCES_FAILED", "Failed to format Airline Preference record.");
        } catch (FeatureManagementException uare) {
            logger.error("FeatureManagementException: {}", uare.getMessage(), uare);
            resultObj = new ApiError("AIRLINE_PREFERENCES_FAILED", "Unable to locate records.");
        } finally {

            if (resultObj instanceof ApiError) {
                logger.error("FDAGndSvcLog> {}", ControllerUtils.sanitizeString(progressLog.toString()));
            }
        }

        return resultObj;
    }

    public String getTSP(String airline) throws IOException, TspConfigLogException, FileDownloadException {
        logger.info("Auto Config package check starting..");

        Date lastModified = null;

        String airlineGroup = airline.replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);
        logger.info("User belongs to: {}", airlineGroup);

        try {
            AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"),
                    this.appProps.get("StorageKey"));

            String container = TSP_CONFIG_ZIP_CONTAINER;

            // File path name to retrieve from blob - it is not truly a real directory
            String fileName = new StringBuilder(airlineGroup).append("/").append(airlineGroup).append("-config-pkg.zip")
                    .toString();
            boolean tspExists = asu.blobExistsOnCloud(container, fileName);

            logger.info("TSP File Name: {}", fileName);
            logger.warn("TSP Exists: {}", ((tspExists == true) ? "Yes" : "No"));

            // If tsp exists get last modified
            if (tspExists) {
                lastModified = fileManagementService.getBlobLastModifiedTimeStamp(container, fileName);
                logger.debug("retrieved timestamp: " + lastModified.toString());
            } else {
                lastModified = null;
            }

        } catch (IOException e) {
            logger.error("Error retrieving TSP package: {}", e.getMessage(), e);
        }

        return lastModified.toString();

    }

    public void updateUser(UserAccount newUserPayload, String airline) throws UserAccountRegistrationException {
        // Register new user in the account registration database
        Group membership = new Group();
        membership.setDescription("airline");
        membership.setDisplayName(airline);

        Group role = new Group();
        role.setDescription("role");
        role.setDisplayName(newUserPayload.getUserRole());

        List<Group> groups = new ArrayList<>();
        groups.add(membership);
        groups.add(role);

        newUserPayload.setGroups(groups);
        
        userAccountRegister.updateUserAccount(newUserPayload);
        
        logger.info("Updated [{}] in database", newUserPayload.getMailNickname());
    
    }
}