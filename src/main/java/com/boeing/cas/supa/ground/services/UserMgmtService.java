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

    public Object getUsers(String accessTokenInRequest, String airline) {

        Object resultObj = null;
        StringBuilder progressLog = new StringBuilder("Get Users -");

        try {

            // Get group membership of user issuing request. Ensure that user belongs to
            // role-airlinefocal group
            // and one and only one airline group.
            User airlineFocalCurrentUser = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);

            List<UserAccount> users = userAccountRegister.getAllUsers(airline, airlineFocalCurrentUser.getObjectId());

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

        int returnVal = userAccountPreregister.registerNewUserAccount(newUserPayload);
        if (returnVal == 1) {
            logger.info("Registered new user {} in database");
        } else {
            logger.error("failure to create");

        }
        return returnVal;
    }

    public int updatePreUser(PreUserAccount newUserPayload) throws UserAccountRegistrationException {
        // Register new user in the account registration database
        String registrationToken = UUID.randomUUID().toString();

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

    public Object deleteUser(String userId, String accessTokenInRequest, boolean isSuperAdmin, String membership,
            String role) {

        Object resultObj = null;
        StringBuilder progressLog = new StringBuilder("Delete user -");

        try {

            // Get group membership of user issuing request. Ensure that user belongs to
            // role-airlinefocal group
            // and one and only one airline group.
            User airlineFocalCurrentUser = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);

            // Ensure requesting user is not trying to delete self!
            if (airlineFocalCurrentUser.getObjectId().equals(userId)) {
                return new ApiError("USER_DELETE_FAILED", "User cannot delete self", RequestFailureReason.BAD_REQUEST);
            }

            // Validate user privileges by checking group membership. User must either:
            // - perform operation in superadmin mode and have User Account Administrator
            // directory role
            // -or-
            // - belong to Role-AirlineFocal group and a single Airline group
            if (!role.equals("role-airlinefocal") || membership.isEmpty()) {
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

    public Object createUser(NewUser newUserPayload, String accessTokenInRequest, Group airlineGroup,
            String roleGroupName, boolean newRegistrationProcess, String airline) {

        Object resultObj = null;
        StringBuilder progressLog = new StringBuilder("Create user -");

        // Validate the user name
        if (StringUtils.isBlank(newUserPayload.getUserPrincipalName())
                || !UsernamePolicyEnforcer.validate(newUserPayload.getUserPrincipalName())) {
            logger.error("Missing or invalid username specified in user creation request: {}",
                    ControllerUtils.sanitizeString(newUserPayload.getUserPrincipalName()));
            return new ApiError("CREATE_USER_FAILURE", UsernamePolicyEnforcer.ERROR_USERNAME_FAILED_DESCRIPTION,
                    RequestFailureReason.BAD_REQUEST);
        }
        // Validate the password
        else if (StringUtils.isBlank(newUserPayload.getPassword())
                || !PasswordPolicyEnforcer.validate(newUserPayload.getPassword())) {
            logger.error("Missing or invalid password specified in user creation request: {}",
                    ControllerUtils.sanitizeString(newUserPayload.getPassword()));
            return new ApiError("CREATE_USER_FAILURE", PasswordPolicyEnforcer.ERROR_PSWD_FAILED_DESCRIPTION,
                    RequestFailureReason.BAD_REQUEST);
        }
        // Validate the first name
        else if (StringUtils.isBlank(newUserPayload.getGivenName())) {
            logger.error("Missing or invalid first name specified in user creation request: {}",
                    ControllerUtils.sanitizeString(newUserPayload.getGivenName()));
            return new ApiError("CREATE_USER_FAILURE", "Missing or invalid first name",
                    RequestFailureReason.BAD_REQUEST);
        }
        // Validate the last name
        else if (StringUtils.isBlank(newUserPayload.getSurname())) {
            logger.error("Missing or invalid last name specified in user creation request: {}",
                    ControllerUtils.sanitizeString(newUserPayload.getSurname()));
            return new ApiError("CREATE_USER_FAILURE", "Missing or invalid last name",
                    RequestFailureReason.BAD_REQUEST);
        }
        // Validate the email address
        else if (CollectionUtils.isEmpty(newUserPayload.getOtherMails()) || newUserPayload.getOtherMails().stream()
                .anyMatch(e -> StringUtils.isBlank(e) || !e.matches(Constants.PATTERN_EMAIL_REGEX))) {
            logger.error("Missing or invalid work email: {}",
                    CollectionUtils.isEmpty(newUserPayload.getOtherMails())
                            ? StringUtils.EMPTY
                            : ControllerUtils
                                    .sanitizeString(StringUtils.join(newUserPayload.getOtherMails().toArray())));
            return new ApiError("CREATE_USER_FAILURE", "Missing or invalid work email",
                    RequestFailureReason.BAD_REQUEST);
        }
        // Validate the user role specified
        else if (StringUtils.isBlank(newUserPayload.getRoleGroupName())
                || !Constants.ALLOWED_USER_ROLES.contains(newUserPayload.getRoleGroupName())) {
            logger.error("Missing or invalid user role specified in user creation request: {}",
                    ControllerUtils.sanitizeString(newUserPayload.getRoleGroupName()));
            return new ApiError("CREATE_USER_FAILURE", "Missing or invalid user role",
                    RequestFailureReason.BAD_REQUEST);
        }
        // validate airline group specifier, either in the new user request payload or
        // in the airline group argument
        else if (airlineGroup == null && StringUtils.isBlank(newUserPayload.getAirlineGroupName())) {
            logger.error("Missing or invalid airline group specified in user creation request: {}",
                    ControllerUtils.sanitizeString(newUserPayload.getAirlineGroupName()));
            return new ApiError("CREATE_USER_FAILURE", "Missing or invalid airline", RequestFailureReason.BAD_REQUEST);
        } else {
            // continue with deeper validations...
        }

        // Get GroupId of requested user role
        Group roleGroup = aadClient.getGroupByName(newUserPayload.getRoleGroupName(), accessTokenInRequest);
        if (roleGroup == null) {
            logger.error("Failed to retrieve user role group: {}",
                    ControllerUtils.sanitizeString(newUserPayload.getRoleGroupName()));
            return new ApiError("CREATE_USER_FAILURE", "Missing or invalid user role");
        }

        // Get Group object corresponding to requested airline group
        if (airlineGroup == null) {

            User requestorUser = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);
            if (requestorUser.getDirectoryRoles() != null) {
                List<DirectoryRole> directoryRoles = requestorUser.getDirectoryRoles().stream()
                        .filter(g -> g.getDisplayName().toLowerCase().equals("user administrator"))
                        .collect(Collectors.toList());
                if (directoryRoles.size() == 0) {
                    logger.error("Insufficient privileges to create user: not a user account administrator");
                    return new ApiError("CREATE_USER_FAILURE", "Insufficient privileges to create user",
                            RequestFailureReason.UNAUTHORIZED);
                }
                // Get GroupId of requested airline group
                airlineGroup = aadClient.getGroupByName(newUserPayload.getAirlineGroupName(), accessTokenInRequest);
                if (airlineGroup == null) {
                    logger.error("Insufficient privileges to create user: failed to retrieve airline group");
                    return new ApiError("CREATE_USER_FAILURE", "Missing or invalid airline",
                            RequestFailureReason.BAD_REQUEST);
                }
            }
        }

        // Get access token based on delegated permission via impersonation with a Local
        // Administrator of the tenant.
        String adminAccessToken = null;
        Object authResult = aadClient.getElevatedPermissionsAccessToken(PermissionType.IMPERSONATION);
        if (authResult instanceof ApiError) {
            return authResult;
        }
        // access token could not be obtained either via delegated permissions or
        // application permissions.
        else if (authResult == null) {
            return new ApiError("CREATE_USER_FAILURE", "Failed to acquire permissions");
        } else {
            adminAccessToken = String.valueOf(authResult);
            progressLog.append("\nObtained impersonation access token");
        }

        // Validate user name provided by the admin; if invalid, return appropriate
        // error key/description
        User userObj = null;
        try {

            userObj = aadClient.getUserFromGraph(
                    new StringBuilder(newUserPayload.getUserPrincipalName()).append('@')
                            .append(this.appProps.get("AzureADCustomTenantName")).toString(),
                    adminAccessToken,
                    progressLog,
                    "CREATE_USER_ERROR");
            if (userObj != null) {
                logger.warn("User already exists: {}",
                        ControllerUtils.sanitizeString(newUserPayload.getUserPrincipalName()));
                return new ApiError("CREATE_USER_ERROR",
                        "Username already exists in directory. Please use an altered form of the username, such as appending number(s), to avoid name conflicts.",
                        RequestFailureReason.CONFLICT);
            } else {
                // proceed with creation of user account
            }
        } catch (ApiErrorException aee) {

            if (aee.getApiError().getFailureReason().equals(RequestFailureReason.NOT_FOUND)) {
                // this is good, user does not exist, so can proceed with user creation
            } else {
                logger.warn("User could not be retrieved from graph: {}",
                        ControllerUtils.sanitizeString(aee.getApiError().getErrorDescription()));
                return aee.getApiError();
            }
        } catch (IOException ioe) {
            logger.warn("User could not be retrieved from graph: {}", ControllerUtils.sanitizeString(ioe.getMessage()),
                    ioe);
            return new ApiError("CREATE_USER_ERROR", "Failed to retrieve user from graph",
                    RequestFailureReason.INTERNAL_SERVER_ERROR);
        }

        // Proceed with request to create the new user account
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = aadClient.convertNewUserObjectToJsonPayload(mapper, newUserPayload);
        try {

            URL url = new URL(
                    new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
                            .append("/users")
                            .append('?').append(new StringBuilder(Constants.AZURE_API_VERSION_PREFIX)
                                    .append(azureadApiVersion).toString())
                            .toString());

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            // Set the appropriate header fields in the request header.
            conn.setRequestMethod("POST");
            conn.setRequestProperty("api-version", azureadApiVersion);
            conn.setRequestProperty("Authorization", adminAccessToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", Constants.ACCEPT_CT_JSON_ODATAMINIMAL);
            conn.setDoOutput(true);
            try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                dos.writeBytes(mapper.writeValueAsString(rootNode));
                dos.flush();
            }

            String responseStr = HttpClientHelper.getResponseStringFromConn(conn,
                    conn.getResponseCode() == HttpStatus.CREATED.value());
            progressLog.append("\nReceived HTTP ").append(conn.getResponseCode()).append(", response: ")
                    .append(responseStr);
            if (conn.getResponseCode() == HttpStatus.CREATED.value()) {

                mapper = new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .setSerializationInclusion(Include.NON_NULL);
                resultObj = mapper.readValue(responseStr, User.class);
                progressLog.append("\nDeserialized create user response to obtain User object");

                User newlyCreatedUser = (User) resultObj;
                logger.info("Created new user: {}", newlyCreatedUser.getUserPrincipalName());
                // Assign user to airline-related group and role-related group
                boolean addedUserToAirlineGroup = aadClient.addUserToGroup(airlineGroup.getObjectId(),
                        newlyCreatedUser.getObjectId(), adminAccessToken);
                progressLog.append("\nAdded user to airline group");
                boolean addedUserToRoleGroup = aadClient.addUserToGroup(roleGroup.getObjectId(),
                        newlyCreatedUser.getObjectId(),
                        adminAccessToken);
                progressLog.append("\nAdded user to primary user role");

                // Get the updated user membership and update the User object sent back in the
                // response.
                UserMembership userMembership = aadClient.getUserMembershipFromGraph(newlyCreatedUser.getObjectId(),
                        adminAccessToken);
                progressLog.append("\nObtained groups which user is a member of");
                newlyCreatedUser.setGroups(userMembership.getUserGroups());

                if (!addedUserToAirlineGroup || !addedUserToRoleGroup) {
                    logger.error("Failed to add user to airline group and/or role group");
                }
                progressLog.append("\nUser added to airline and role groups");

                // User creation looks good... set new user to the return value [resultObj]
                UserAccount newUser = new UserAccount(newlyCreatedUser);
                newUser.setUserRole(ControllerUtils.sanitizeString(roleGroupName));

                // resultObj = newlyCreatedUser;
                resultObj = newUser;
                // logger.info("Added new user {} to {} and {} groups",
                // newlyCreatedUser.getUserPrincipalName(), airlineGroup.getDisplayName(),
                // ControllerUtils.sanitizeString(roleGroupName));
                logger.info("Added new user {} to {} and {} groups", newUser.getUserPrincipalName(),
                        airlineGroup.getDisplayName(), ControllerUtils.sanitizeString(roleGroupName));

                // Register new user in the account registration database
                String registrationToken = UUID.randomUUID().toString();
                UserAccountRegistration registration = new UserAccountRegistration(registrationToken,
                        newlyCreatedUser.getObjectId(), newlyCreatedUser.getUserPrincipalName(),
                        airlineGroup.getDisplayName(), newUserPayload.getOtherMails().get(0),
                        Constants.UserAccountState.PENDING_USER_ACTIVATION.toString());
                userAccountRegister.registerNewUserAccount(registration);
                userAccountRegister.updateUserAccount(newlyCreatedUser);
                progressLog.append("\nRegistered new user account in database");
                logger.info("Registered new user {} in the account registration database",
                        newlyCreatedUser.getUserPrincipalName());

                // New user account registration is successful. Now send email to user (use
                // otherMail address)
                MimeMessage message = emailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                // MimeMessageHelper helper = new MimeMessageHelper(message);
                String azureDomain = this.appProps.get("AzureADCustomTenantName").toString();
                helper.setFrom("noreply@flitedeckadvisor.com");
                helper.setReplyTo(
                        new StringBuilder("noreply@").append(this.appProps.get("AzureADCustomTenantName")).toString());
                if (azureDomain.equals("flitedeckadvisor.com")) {
                    helper.setSubject("Welcome to FliteDeck Advisor - New Account Registration");
                } else {
                    helper.setSubject("(TEST ENV) Welcome to FliteDeck Advisor - New Account Registration");
                }
                helper.setTo(newUserPayload.getOtherMails().get(0));

                helper.setText(aadClient.composeNewUserAccountActivationEmail(newlyCreatedUser, registrationToken,
                        newRegistrationProcess), true);

                File emailNewPdfAttachment = aadClient.getFileFromBlob("email-new-instructions",
                        "FDA_registration_instructions.pdf",
                        airlineGroup.getDisplayName().substring(new String("airline-").length()));
                File emailOldPdfAttachment = aadClient.getFileFromBlob("email-instructions",
                        "FDA_registration_instructions.pdf",
                        airlineGroup.getDisplayName().substring(new String("airline-").length()));

                // File emailNewPdfAttachment = getFileFromBlob("email-new-instructions",
                // "FDA_registration_instructions.pdf",
                // airlineGroup.getDisplayName().substring(new String("airline-").length()));

                if (newRegistrationProcess) {
                    logger.debug("Using new Process, do NOT attach mp");
                    logger.debug("display name: {}", airlineGroup.getDisplayName());
                    if (!airline.equals("airline-etd")) {
                        logger.debug("Add attachment for airline: \"" + airlineGroup.getDisplayName() + "\"");
                        helper.addAttachment(emailNewPdfAttachment.getName(), emailNewPdfAttachment);
                        logger.debug("attach pdf instructions email [{}]",
                                newRegistrationProcess ? emailNewPdfAttachment.getAbsolutePath()
                                        : emailOldPdfAttachment.getAbsolutePath());

                        String emailPath = newRegistrationProcess ? emailNewPdfAttachment.getAbsolutePath()
                                : emailOldPdfAttachment.getAbsolutePath();
                        logger.debug("attach pdf instructions email [{}]", emailPath);
                    } else {
                        logger.debug("Do not add attachment for airline: \"" + airlineGroup.getDisplayName() + "\"");
                    }
                } else {
                    String mpFileName = newlyCreatedUser.getDisplayName().replaceAll("\\s+", "_").toLowerCase() + ".mp";
                    File emailMpAttachment = aadClient.getFileFromBlob("tmp", mpFileName, null);
                    helper.addAttachment(emailOldPdfAttachment.getName(), emailOldPdfAttachment);
                    logger.debug("Using old Process, attach mp email [{}]", emailMpAttachment.getAbsolutePath());
                    helper.addAttachment(emailMpAttachment.getName(), emailMpAttachment);
                }

                emailSender.send(message);
                logger.info("Sent account activation email to new user {}", newlyCreatedUser.getUserPrincipalName());
                progressLog.append("\nSuccessfully queued email for delivery");
            } else {
                JsonNode errorNode = mapper.readTree(responseStr).path("odata.error");
                JsonNode messageNode = errorNode.path("message");
                JsonNode valueNode = messageNode.path("value");

                resultObj = new ApiError("USER_CREATE_FAILED", valueNode.asText());
            }
        } catch (UserAccountRegistrationException uare) {
            logger.error("Failed to register new user account: {}", uare.getMessage(), uare);
            resultObj = new ApiError("USER_CREATE_FAILED", "FDAGNDSVCERR0001");
        } catch (JsonProcessingException jpe) {
            logger.error("JsonProcessingException: {}", jpe.getMessage(), jpe);
            resultObj = new ApiError("USER_CREATE_FAILED", "FDAGNDSVCERR0002");
        } catch (MalformedURLException murle) {
            logger.error("MalformedURLException: {}", murle.getMessage(), murle);
            resultObj = new ApiError("USER_CREATE_FAILED", "FDAGNDSVCERR0004");
        } catch (ProtocolException pe) {
            logger.error("ProtocolException: {}", pe.getMessage(), pe);
            resultObj = new ApiError("USER_CREATE_FAILED", "FDAGNDSVCERR0008");
        } catch (IOException ioe) {
            logger.error("IOException: {}", ioe.getMessage(), ioe);
            resultObj = new ApiError("USER_CREATE_FAILED", "FDAGNDSVCERR0016");
        } catch (MailException me) {
            logger.error("Failed to send email: {}", me.getMessage(), me);
            resultObj = new ApiError("USER_CREATE_FAILED", "FDAGNDSVCERR0032");
        } catch (FileDownloadException dwn) {
            logger.error("Failed to download Blobs: {}", dwn.getMessage(), dwn);
            resultObj = new ApiError("USER_CREATE_FAILED", "FDAGNDSVCERR0064");
        } catch (Exception e) {

            Throwable nestedException = null;
            if ((nestedException = e.getCause()) != null) {
                logger.error("Failed to complete user creation flow: {}", nestedException.getMessage(),
                        nestedException);
            } else {
                logger.error("Failed to complete user creation flow: {}", e.getMessage(), e);
            }
            resultObj = new ApiError("USER_CREATE_FAILED", "FDAGNDSVCERR0064");
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

}