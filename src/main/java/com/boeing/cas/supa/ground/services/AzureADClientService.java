package com.boeing.cas.supa.ground.services;

import com.boeing.cas.supa.ground.dao.FeatureManagementDao;
import com.boeing.cas.supa.ground.dao.PowerBiInformationDao;
import com.boeing.cas.supa.ground.dao.UserAccountRegistrationDao;
import com.boeing.cas.supa.ground.exceptions.*;
import com.boeing.cas.supa.ground.helpers.AzureADClientHelper;
import com.boeing.cas.supa.ground.helpers.HttpClientHelper;
import com.boeing.cas.supa.ground.pojos.*;
import com.boeing.cas.supa.ground.utils.*;
import com.boeing.cas.supa.ground.utils.Constants.PermissionType;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
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

@Service
public class AzureADClientService {

	private final Logger logger = LoggerFactory.getLogger(AzureADClientService.class);

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
	private FeatureManagementDao featureManagementDao;

	@Autowired
	private PowerBiInformationDao powerBiInformationDao;

	@Autowired
	private EmailRegistrationService emailService;

	public Object getAccessTokenFromUserCredentials(String username, String password, String authority, String clientId) {

        AuthenticationResult result = null;
        ExecutorService service = null;

        try {
        	service = Executors.newFixedThreadPool(1);
        	AuthenticationContext context = new AuthenticationContext(authority, false, service);
            Future<AuthenticationResult> future = context.acquireToken(azureadApiUri, clientId, username, password, null);
            result = future.get();
        } catch (MalformedURLException murle) {
        	logger.error("MalformedURLException: {}", murle.getMessage(), murle);
		} catch (InterruptedException ie) {
        	logger.error("InterruptedException: {}", ie.getMessage(), ie);
        	Thread.currentThread().interrupt();
		} catch (ExecutionException ee) {
			Throwable cause = ee.getCause();
			if (cause != null) {
	        	logger.error("ExecutionException cause: {}", cause.getMessage(), ee);
			}
		} catch (AuthenticationException ae) {
        	logger.error("AuthenticationException: {}", ae.getMessage(), ae);
		} finally {
            if (service != null) { service.shutdown(); }
        }

        return result;
	}

	public Object getAccessTokenFromUserCredentials(String username, String password) {

		Object result = null;
		ExecutorService service = null;

		try {
			service = Executors.newFixedThreadPool(1);
			AuthenticationContext context = new AuthenticationContext(this.appProps.get("AzureADTenantAuthEndpoint"), false, service);
			Future<AuthenticationResult> future = context.acquireToken(azureadApiUri, this.appProps.get("AzureADAppClientID"), username,
					password, null);
			result = future.get();
		} catch (MalformedURLException murle) {
			logger.error("MalformedURLException: {}", murle.getMessage(), murle);
		} catch (InterruptedException ie) {
			logger.error("InterruptedException: {}", ie.getMessage(), ie);
			Thread.currentThread().interrupt();
		} catch (ExecutionException ee) {
			Throwable cause = ee.getCause();
			if (cause != null) {
				logger.error("ExecutionException cause: {}", cause.getMessage(), ee);
				if (cause instanceof AuthenticationException) {
					result = new AuthenticationException(cause.getMessage());
				}
			}
		} catch (AuthenticationException ae) {
			logger.error("AuthenticationException: {}", ae.getMessage(), ae);
			result = new AuthenticationException(ae.getMessage());
		} catch (Exception e) {
			logger.error("Unrecognized Exception: {}", e.getMessage(), e);
			throw e;
		} finally {
			if (service != null) {
				service.shutdown();
			}
		}

		return result;
	}
	
	public String getAccessTokenForApplication() throws MalformedURLException, ExecutionException, InterruptedException {

		AuthenticationResult result = null;
		ExecutorService service = null;
		
		try {

			service = Executors.newFixedThreadPool(1);
			AuthenticationContext context = new AuthenticationContext(this.appProps.get("AzureADTenantAuthEndpoint"), false, service);
			Future<AuthenticationResult> future =
					context.acquireToken(
							azureadApiUri,
							new ClientCredential(this.appProps.get("UserManagementAppClientId"), this.appProps.get("UserManagementAppClientSecret")),
							null);

			result = future.get();
		}
		finally {
			if (service != null) {
				service.shutdown();
			}
		}

		logger.debug("-------> Application token: {}", result.getAccessToken());

		return result.getAccessToken();
	}

	private Object getElevatedPermissionsAccessToken(PermissionType permissionType) {

		// Obtain access token for application, so it can invoke Azure AD Graph API
		String accessToken = null;

		if (permissionType == PermissionType.APPLICATION) {

			// Get access token based on application permission.
			try {
				accessToken = getAccessTokenForApplication();
			} catch (MalformedURLException | ExecutionException | InterruptedException e) {
				logger.error("Application failed to obtain access token for Azure AD Graph API: {}", e.getMessage(), e);
				return new ApiError("PERMISSIONS_FAILURE", e.getMessage());
			}
		}
		else if (permissionType == PermissionType.IMPERSONATION) {

			// Get access token based on delegated permission via impersonation with a Local Administrator of the tenant.
			Object authResult = getAccessTokenFromUserCredentials(this.appProps.get("UserManagementAdminUsername"), this.appProps.get("UserManagementAdminPassword"));
			if (authResult != null && authResult instanceof AuthenticationResult) {
				accessToken = ((AuthenticationResult) authResult).getAccessToken();
			} else {
				logger.error("Failed to obtain access tokens via impersonation");
				return new ApiError("PERMISSIONS_FAILURE", "Failed to impersonate admin user");
			}
		}
		else {
			return new ApiError("PERMISSIONS_FAILURE", "Missing or invalid permissions context");
		}

		return accessToken;
	}

	public AccessToken loginUserForAccessToken(Credential cred) throws UserAuthenticationException {

		Object ar = getAccessTokenFromUserCredentials(cred.getAzUsername(), cred.getAzPassword());
		if (ar != null && ar instanceof AuthenticationResult) {

			AuthenticationResult result = (AuthenticationResult) ar;
			// Get Airline and list of roles from authentication result which encapsulates the access token and user object ID
			UserMembership userMembership = getUserMembershipFromGraph(result.getUserInfo().getUniqueId(), result.getAccessToken());
			//  -> Extract airline group
			List<Group> userAirlineGroups = userMembership.getUserGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
			//  -> Extract list of roles
			List<Group> userRoleGroups = userMembership.getUserGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_USER_ROLE_PREFIX)).collect(Collectors.toList());
			String groupName = userAirlineGroups.size() == 1 ? userAirlineGroups.get(0).getDisplayName() : null;
			List<String> roleNames = userRoleGroups.stream().map(g -> g.getDisplayName()).collect(Collectors.toList());
			// Article ref: //https://stackoverflow.com/questions/31971673/how-can-i-get-a-pem-base-64-from-a-pfx-in-java
			return new AccessToken(result, groupName, roleNames);
		} else if (ar != null && ExceptionUtils.indexOfThrowable((Throwable) ar, AuthenticationException.class) >= 0) {

			ApiError apiError = AzureADClientHelper.getLoginErrorFromString(((AuthenticationException) ar).getMessage());
			if (apiError.getErrorDescription().matches(".*AADSTS50034.*")
				|| apiError.getErrorDescription().matches(".*AADSTS70fdfd002.*")) {

				apiError.setErrorLabel("USER_AUTH_FAILURE");
				apiError.setErrorDescription("Invalid username or password");
			} else if (apiError.getErrorDescription().matches(".*AADSTS50055.*")) {
				
				apiError.setErrorLabel("USER_AUTH_FAILURE");
				apiError.setErrorDescription("User password is expired, force change password");
			}
			apiError.setFailureReason(RequestFailureReason.UNAUTHORIZED);
			logger.warn("Failed authentication: {}", apiError.getErrorDescription());
			throw new UserAuthenticationException(apiError);
		} else if (ar != null && ExceptionUtils.indexOfType((Throwable) ar, Exception.class) >= 0) {

			ApiError apiError = AzureADClientHelper.getLoginErrorFromString(((Exception) ar).getMessage());
			apiError.setFailureReason(RequestFailureReason.UNAUTHORIZED);
			logger.warn("Failed authentication: {}", apiError.getErrorDescription());
			throw new UserAuthenticationException(apiError);
		} else {

			// The code should not reach this block
			logger.warn("Failed authentication: Unable to determine authentication failure error");
			throw new UserAuthenticationException(new ApiError("USER_AUTH_FAILURE", "Unable to determine authentication failure reason", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}

	public Group getGroupByName(String groupName, String applicationToken) {

		Group group = null;

		try {

			URL url = new URL(new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
					.append("/groups")
					.append('?').append(new StringBuilder(Constants.AZURE_API_VERSION_PREFIX).append(azureadApiVersion)
							.toString())
					.append("&$filter=").append(URLEncoder.encode(String.format("displayName eq '%s'", groupName), StandardCharsets.UTF_8.name()))
					.toString());

			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			// Set the appropriate header fields in the request header.
			conn.setRequestMethod("GET");
			conn.setRequestProperty("api-version", azureadApiVersion);
			conn.setRequestProperty("Authorization", applicationToken);
			conn.setRequestProperty("Accept", Constants.ACCEPT_CT_JSON_ODATAMINIMAL);
			
			String responseStr = HttpClientHelper.getResponseStringFromConn(conn, conn.getResponseCode() == HttpStatus.OK.value());
			
			if (conn.getResponseCode() == HttpStatus.OK.value()) {

				ObjectMapper mapper = new ObjectMapper();
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				JsonNode valuesNode = mapper.readTree(responseStr).path("value");
				if (valuesNode.isArray()) {
					group = mapper.treeToValue(valuesNode.get(0), Group.class);
				}
			}
		}
		catch (Exception e) {
			logger.error("Failed to query groups: {}", e.getMessage(), e);
		}
		
		return group;
	}

	public Object createUser(NewUser newUserPayload, String accessTokenInRequest, Group airlineGroup,
							 						  String roleGroupName, boolean newRegistrationProcess) {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Create user -");

		// Validate the user name
		if (StringUtils.isBlank(newUserPayload.getUserPrincipalName()) || !UsernamePolicyEnforcer.validate(newUserPayload.getUserPrincipalName())) {
			logger.error("Missing or invalid username specified in user creation request: {}", ControllerUtils.sanitizeString(newUserPayload.getUserPrincipalName()));
			return new ApiError("CREATE_USER_FAILURE", UsernamePolicyEnforcer.ERROR_USERNAME_FAILED_DESCRIPTION, RequestFailureReason.BAD_REQUEST);
		}
		// Validate the password
		else if (StringUtils.isBlank(newUserPayload.getPassword()) || !PasswordPolicyEnforcer.validate(newUserPayload.getPassword())) {
			logger.error("Missing or invalid password specified in user creation request: {}", ControllerUtils.sanitizeString(newUserPayload.getPassword()));
			return new ApiError("CREATE_USER_FAILURE", PasswordPolicyEnforcer.ERROR_PSWD_FAILED_DESCRIPTION, RequestFailureReason.BAD_REQUEST);
		}
		// Validate the first name
		else if (StringUtils.isBlank(newUserPayload.getGivenName())) {
			logger.error("Missing or invalid first name specified in user creation request: {}", ControllerUtils.sanitizeString(newUserPayload.getGivenName()));
			return new ApiError("CREATE_USER_FAILURE", "Missing or invalid first name", RequestFailureReason.BAD_REQUEST);
		}
		// Validate the last name
		else if (StringUtils.isBlank(newUserPayload.getSurname())) {
			logger.error("Missing or invalid last name specified in user creation request: {}", ControllerUtils.sanitizeString(newUserPayload.getSurname()));
			return new ApiError("CREATE_USER_FAILURE", "Missing or invalid last name", RequestFailureReason.BAD_REQUEST);
		}
		// Validate the email address
		else if (CollectionUtils.isEmpty(newUserPayload.getOtherMails()) || newUserPayload.getOtherMails().stream().anyMatch(e -> StringUtils.isBlank(e) || !e.matches(Constants.PATTERN_EMAIL_REGEX))) {
			logger.error("Missing or invalid work email: {}",
					CollectionUtils.isEmpty(newUserPayload.getOtherMails())
						? StringUtils.EMPTY
						: ControllerUtils.sanitizeString(StringUtils.join(newUserPayload.getOtherMails().toArray())));
			return new ApiError("CREATE_USER_FAILURE", "Missing or invalid work email", RequestFailureReason.BAD_REQUEST);
		}
		// Validate the user role specified
		else if (StringUtils.isBlank(newUserPayload.getRoleGroupName()) || !Constants.ALLOWED_USER_ROLES.contains(newUserPayload.getRoleGroupName())) {
			logger.error("Missing or invalid user role specified in user creation request: {}", ControllerUtils.sanitizeString(newUserPayload.getRoleGroupName()));
			return new ApiError("CREATE_USER_FAILURE", "Missing or invalid user role", RequestFailureReason.BAD_REQUEST);
		}
		// validate airline group specifier, either in the new user request payload or in the airline group argument
		else if (airlineGroup == null && StringUtils.isBlank(newUserPayload.getAirlineGroupName())) {
			logger.error("Missing or invalid airline group specified in user creation request: {}", ControllerUtils.sanitizeString(newUserPayload.getAirlineGroupName()));
			return new ApiError("CREATE_USER_FAILURE", "Missing or invalid airline", RequestFailureReason.BAD_REQUEST);
		}
		else {
			// continue with deeper validations...
		}

		// Get GroupId of requested user role
		Group roleGroup = getGroupByName(newUserPayload.getRoleGroupName(), accessTokenInRequest);
		if (roleGroup == null) {
			logger.error("Failed to retrieve user role group: {}", ControllerUtils.sanitizeString(newUserPayload.getRoleGroupName()));
			return new ApiError("CREATE_USER_FAILURE", "Missing or invalid user role");
		}
		
		// Get Group object corresponding to requested airline group
		if (airlineGroup == null) {

			User requestorUser = getUserInfoFromJwtAccessToken(accessTokenInRequest);
			if (requestorUser.getDirectoryRoles() != null) {
				List<DirectoryRole> directoryRoles = requestorUser.getDirectoryRoles().stream().filter(g -> g.getDisplayName().toLowerCase().equals("user account administrator")).collect(Collectors.toList());
				if (directoryRoles.size() == 0) {
					logger.error("Insufficient privileges to create user: not a user account administrator");
					return new ApiError("CREATE_USER_FAILURE", "Insufficient privileges to create user", RequestFailureReason.UNAUTHORIZED);
				}
				// Get GroupId of requested airline group
				airlineGroup = getGroupByName(newUserPayload.getAirlineGroupName(), accessTokenInRequest);
				if (airlineGroup == null) {
					logger.error("Insufficient privileges to create user: failed to retrieve airline group");
					return new ApiError("CREATE_USER_FAILURE", "Missing or invalid airline", RequestFailureReason.BAD_REQUEST);
				}
			}
		}

		// Get access token based on delegated permission via impersonation with a Local Administrator of the tenant.
		String adminAccessToken = null;
		Object authResult = getElevatedPermissionsAccessToken(PermissionType.IMPERSONATION);
		if (authResult instanceof ApiError) {
			return authResult;
		}
		// access token could not be obtained either via delegated permissions or application permissions.
		else if (authResult == null) {
			return new ApiError("CREATE_USER_FAILURE", "Failed to acquire permissions");
		}
		else {
			adminAccessToken = String.valueOf(authResult);
			progressLog.append("\nObtained impersonation access token");
		}

		// Validate user name  provided by the admin; if invalid, return appropriate error key/description
		User userObj = null;
		try {

			userObj = getUserFromGraph(
					new StringBuilder(newUserPayload.getUserPrincipalName()).append('@').append(this.appProps.get("AzureADCustomTenantName")).toString(),
					adminAccessToken,
					progressLog,
					"CREATE_USER_ERROR");
			if (userObj != null) {
				logger.warn("User already exists: {}", ControllerUtils.sanitizeString(newUserPayload.getUserPrincipalName()));
				return new ApiError("CREATE_USER_ERROR", "Username already exists in directory. Please use an altered form of the username, such as appending number(s), to avoid name conflicts.", RequestFailureReason.CONFLICT);
			} else {
				// proceed with creation of user account
			}
		} catch (ApiErrorException aee) {

			if (aee.getApiError().getFailureReason().equals(RequestFailureReason.NOT_FOUND)) {
				// this is good, user does not exist, so can proceed with user creation
			} else {
				logger.warn("User could not be retrieved from graph: {}", ControllerUtils.sanitizeString(aee.getApiError().getErrorDescription()));
				return aee.getApiError();
			}
		} catch (IOException ioe) {
			logger.warn("User could not be retrieved from graph: {}", ControllerUtils.sanitizeString(ioe.getMessage()), ioe);
			return new ApiError("CREATE_USER_ERROR", "Failed to retrieve user from graph", RequestFailureReason.INTERNAL_SERVER_ERROR);
		}

		// Proceed with request to create the new user account
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode rootNode = convertNewUserObjectToJsonPayload(mapper, newUserPayload);
		try {

			URL url = new URL(new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
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
			
			String responseStr = HttpClientHelper.getResponseStringFromConn(conn, conn.getResponseCode() == HttpStatus.CREATED.value());
			progressLog.append("\nReceived HTTP ").append(conn.getResponseCode()).append(", response: ").append(responseStr);
			if (conn.getResponseCode() == HttpStatus.CREATED.value()) {

				mapper = new ObjectMapper()
						.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
						.setSerializationInclusion(Include.NON_NULL);
				resultObj = mapper.readValue(responseStr, User.class);
				progressLog.append("\nDeserialized create user response to obtain User object");

				User newlyCreatedUser = (User) resultObj;
				logger.info("Created new user: {}", newlyCreatedUser.getUserPrincipalName());
				// Assign user to airline-related group and role-related group
				boolean addedUserToAirlineGroup = addUserToGroup(airlineGroup.getObjectId(), newlyCreatedUser.getObjectId(), adminAccessToken);
				progressLog.append("\nAdded user to airline group");
				boolean addedUserToRoleGroup = addUserToGroup(roleGroup.getObjectId(), newlyCreatedUser.getObjectId(), adminAccessToken);
				progressLog.append("\nAdded user to primary user role");

				// Get the updated user membership and update the User object sent back in the response.
				UserMembership userMembership = getUserMembershipFromGraph(newlyCreatedUser.getObjectId(), adminAccessToken);
				progressLog.append("\nObtained groups which user is a member of");
				newlyCreatedUser.setGroups(userMembership.getUserGroups());

				if (!addedUserToAirlineGroup || !addedUserToRoleGroup) {
					logger.error("Failed to add user to airline group and/or role group");
				}
				progressLog.append("\nUser added to airline and role groups");

				// User creation looks good... set new user to the return value [resultObj]
				UserAccount newUser = new UserAccount(newlyCreatedUser);
				newUser.setUserRole(ControllerUtils.sanitizeString(roleGroupName));

				//resultObj = newlyCreatedUser;
				resultObj = newUser;
				//logger.info("Added new user {} to {} and {} groups", newlyCreatedUser.getUserPrincipalName(), airlineGroup.getDisplayName(), ControllerUtils.sanitizeString(roleGroupName));
				logger.info("Added new user {} to {} and {} groups", newUser.getUserPrincipalName(), airlineGroup.getDisplayName(), ControllerUtils.sanitizeString(roleGroupName));

				// Register new user in the account registration database
				String registrationToken = UUID.randomUUID().toString();
				UserAccountRegistration registration = new UserAccountRegistration(registrationToken, newlyCreatedUser.getObjectId(), newlyCreatedUser.getUserPrincipalName(), airlineGroup.getDisplayName(), newUserPayload.getOtherMails().get(0), Constants.UserAccountState.PENDING_USER_ACTIVATION.toString());
				userAccountRegister.registerNewUserAccount(registration);
				userAccountRegister.updateUserAccount(newlyCreatedUser);
				progressLog.append("\nRegistered new user account in database");
				logger.info("Registered new user {} in the account registration database", newlyCreatedUser.getUserPrincipalName());
				
				// New user account registration is successful. Now send email to user (use otherMail address)
				MimeMessage message = emailSender.createMimeMessage();
				MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
				//MimeMessageHelper helper = new MimeMessageHelper(message);
				helper.setFrom(new StringBuilder("noreply@").append(this.appProps.get("AzureADCustomTenantName")).toString());
				helper.setReplyTo(new StringBuilder("noreply@").append(this.appProps.get("AzureADCustomTenantName")).toString());
				helper.setSubject("Welcome to FliteDeck Advisor - New Account Registration");
				helper.setTo(newUserPayload.getOtherMails().get(0));

				helper.setText(composeNewUserAccountActivationEmail(newlyCreatedUser, registrationToken, newRegistrationProcess), true);

				File emailNewPdfAttachment = getFileFromBlob("email-new-instructions",
						"FDA_registration_instructions.pdf", airlineGroup.getDisplayName().substring(new String("airline-").length()));
				File emailOldPdfAttachment = getFileFromBlob("email-instructions",
						"FDA_registration_instructions.pdf", airlineGroup.getDisplayName().substring(new String("airline-").length()));

				//File emailNewPdfAttachment = getFileFromBlob("email-new-instructions",
				//		"FDA_registration_instructions.pdf", airlineGroup.getDisplayName().substring(new String("airline-").length()));

				if (newRegistrationProcess) {
					logger.debug("Using new Process, do NOT attach mp");
					helper.addAttachment(emailNewPdfAttachment.getName(), emailNewPdfAttachment);
				} else {
					String mpFileName = newlyCreatedUser.getDisplayName().replaceAll("\\s+", "_").toLowerCase() + ".mp";
					File emailMpAttachment = getFileFromBlob("tmp", mpFileName, null);
					helper.addAttachment(emailOldPdfAttachment.getName(), emailOldPdfAttachment);
					logger.debug("Using old Process, attach mp email [{}]", emailMpAttachment.getAbsolutePath());
					helper.addAttachment(emailMpAttachment.getName(), emailMpAttachment);
				}
				logger.debug("attach pdf instructions email [{}]", newRegistrationProcess?
						emailNewPdfAttachment.getAbsolutePath(): emailOldPdfAttachment.getAbsolutePath());

				String emailPath = newRegistrationProcess ? emailNewPdfAttachment.getAbsolutePath() : emailOldPdfAttachment.getAbsolutePath();
				logger.debug("attach pdf instructions email [{}]", emailPath);

				emailSender.send(message);
				logger.info("Sent account activation email to new user {}", newlyCreatedUser.getUserPrincipalName());
				progressLog.append("\nSuccessfully queued email for delivery");
			}
			else {
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
				logger.error("Failed to complete user creation flow: {}", nestedException.getMessage(), nestedException);
			}
			else {
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

	public Object getPowerBiReport(String accessTokenInRequest) {

		Object resultObj = null;
		try {
			// Get group membership of user issuing request. Ensure that user belongs to role-airlinefocal group
			// and one and only one airline group.
			User airlineFocalCurrentUser = getUserInfoFromJwtAccessToken(accessTokenInRequest);

			String airline = "unknown";

			// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
			List<Group> airlineGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());
			if (airlineGroups.size() != 1) {
				return new ApiError("USERS_LIST_FAILED", "User membership is ambiguous, airlines[" + airlineGroups.size() + "]", RequestFailureReason.UNAUTHORIZED);
			} else {
				airline = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);
			}
			logger.info("Obtain Power Bi Report for airline [{}]", airline);
			PowerBiInformation powerBiInformation = powerBiInformationDao.getPowerBiInformation(airline);

			StringBuilder sb = new StringBuilder();
//			StringBuilder sbJavaScript = new StringBuilder();
//			try {
//				File powerBiJavaScriptFile = new ClassPathResource("powerbi/powerbi.js").getFile();
//				BufferedReader reader = new BufferedReader(new FileReader(powerBiJavaScriptFile));
//				String str;
//				while ((str = reader.readLine()) != null)
//				{
//					sbJavaScript.append(str + "\n");
//				}
//				reader.close();
//			} catch (IOException io) {
//				logger.warn(io.getMessage());
//			}
			sb.append("<html>");
			sb.append("<script language=\"javascript\"  type=\"application/javascript\" src=\"https://microsoft.github.io/PowerBI-JavaScript/demo/node_modules/jquery/dist/jquery.js\"></script>");
			sb.append("<script language=\"javascript\"  type=\"application/javascript\" src=\"https://microsoft.github.io/PowerBI-JavaScript/demo/node_modules/powerbi-client/dist/powerbi.js\"></script>");
			sb.append("<script language=\"javascript\">");
			//note: embedded tokens start with H4sI
//			sb.append("/*! --- start of powerbi.js code --- */");
//			sb.append(sbJavaScript.toString());
//			sb.append("/*! --- end of powerbi.js code --- */");
			sb.append("");
			sb.append("window.onload = function() {");
			sb.append("var AccessToken = '" + powerBiInformation.getEmbeddedToken() + "';");
			sb.append("var EmbedUrl = 'https://app.powerbi.com/reportEmbed?reportId=" + powerBiInformation.getReportId()
				+ "&groupId=" + powerBiInformation.getWorkspaceId()  + "';");
				//+ "&config=eyJjbHVzdGVyVXJsIjoiaHR0cHM6Ly9XQUJJLVVTLU5PUlRILUNFTlRSQUwtcmVkaXJlY3QuYW5hbHlzaXMud2luZG93cy5uZXQifQ%3d%3d';");
			sb.append("var EmbedReportId = '" + powerBiInformation.getReportId() + "';");
			//sb.append("console.log('AccessToken:', AccessToken);");
			//sb.append("console.log('EmbedUrl:', EmbedUrl);");
			//sb.append("console.log('EmbedReportId:', EmbedReportId);");

			sb.append("var models = window['powerbi-client'].models;");
			//sb.append("console.log('Models:', models);");

			sb.append("var embedConfiguration  = {");
			sb.append("    type: 'report',");
			sb.append("    tokenType: models.TokenType.Embed,");
			sb.append("    accessToken: AccessToken,");
			sb.append("    embedUrl: EmbedUrl,");
			sb.append("    id: EmbedReportId,");
			sb.append("    permissions: models.Permissions.All,");
			sb.append("    settings: {");
			sb.append("        filterPaneEnabled: true,");
			sb.append("        navContentPaneEnabled: true");
			sb.append("    }");
			sb.append("};");
			sb.append("var $reportContainer = $('#dashboardContainer');");
			sb.append("var report = powerbi.embed($reportContainer.get(0), embedConfiguration);");
			sb.append("}");
			sb.append("</script>");
			sb.append("<div id=\"dashboardContainer\"></div>");
			sb.append("</html>");
			resultObj = sb.toString();

		} catch (PowerBiInformationException jpe) {
			logger.error("PowerBiInformationException: {}", jpe.getMessage(), jpe);
			resultObj = new ApiError("POWER_BI_INFORMATION_FAILED", "Failed to retrieve Power Bi Information.");
		} finally {

			if (resultObj instanceof ApiError) {
				logger.error("Failed to retrieve Power Bi Information  {}");
			}
		}

		return resultObj;
	}


	public Object getUsers(String accessTokenInRequest) {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Get Users -");

		try {

			// Get group membership of user issuing request. Ensure that user belongs to role-airlinefocal group
			 // and one and only one airline group.
			User airlineFocalCurrentUser = getUserInfoFromJwtAccessToken(accessTokenInRequest);

			String airline = "airline-unknown";

			// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
			List<Group> airlineGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());
			//List<Group> roleAirlineFocalGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().equals("role-airlinefocal")).peek(g -> logger.info("Role Group: {}", g)).collect(Collectors.toList());
			//if (roleAirlineFocalGroups.isEmpty()) {
			//	return new ApiError("USERS_LIST_FAILED", "User [role] must be a focal", RequestFailureReason.UNAUTHORIZED);
			//}
			if (airlineGroups.size() != 1 ) {
				return new ApiError("USERS_LIST_FAILED", "User membership is ambiguous, airlines[" + airlineGroups.size() + "]", RequestFailureReason.UNAUTHORIZED);
			} else {
				airline = airlineGroups.get(0).getDisplayName();
			}
			progressLog.append("\nRequesting user belongs to single airline group and airline focal role group");
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


	public Object getRoles(String accessTokenInRequest) {

		Object resultObj = null;
		List<String> roles = new ArrayList<>();

		try {

			URL url = new URL(new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
					.append("/groups")
					.append('?').append(new StringBuilder(Constants.AZURE_API_VERSION_PREFIX).append(azureadApiVersion)
							.toString())
					.toString());

			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			// Set the appropriate header fields in the request header.
			conn.setRequestMethod("GET");
			conn.setRequestProperty("api-version", azureadApiVersion);
			conn.setRequestProperty("Authorization", accessTokenInRequest);
			conn.setRequestProperty("Accept", Constants.ACCEPT_CT_JSON_ODATAMINIMAL);

			String responseStr = HttpClientHelper.getResponseStringFromConn(conn, conn.getResponseCode() == HttpStatus.OK.value());
			if (conn.getResponseCode() == HttpStatus.OK.value()) {

				Group group = null;
				ObjectMapper mapper = new ObjectMapper();
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				JsonNode valuesNode = mapper.readTree(responseStr).path("value");
				if (valuesNode.isArray()) {

					for (final JsonNode objNode : valuesNode) {
						group = mapper.treeToValue(objNode, Group.class);
						if (group.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_USER_ROLE_PREFIX)) {
							roles.add(group.getDisplayName());
						}
					}
				}
			}
			ObjectMapper mapper = new ObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.setSerializationInclusion(Include.NON_NULL);

			logger.info("returned [{}] roles", roles.size());

			resultObj = mapper.setSerializationInclusion(Include.NON_NULL).setSerializationInclusion(Include.NON_EMPTY)
					.writeValueAsString(roles);
		}
		catch (Exception e) {
			logger.error("Failed to query groups: {}", e.getMessage(), e);
		}

		return resultObj;
	}

	public Object getUpdatedClientCert(String accessTokenInRequest) {
		Object resultObj = null;

		User currentUser = getUserInfoFromJwtAccessToken(accessTokenInRequest);
		String registrationToken = "";  // NEED THE TOKEN

		logger.debug(" ===========FDAClientCert2=======");
		String fdadvisorClient1CertBase64 = new StringBuilder(appProps.get("FDAdvisorClient1CertName")).append("base64").toString();
		if (fdadvisorClient1CertBase64 != "") {
			logger.info("CERT IS: {}", fdadvisorClient1CertBase64);

			Object base64EncodedPayload = Base64.getEncoder().encodeToString(
					new StringBuilder(currentUser.getUserPrincipalName()).append(' ').append(registrationToken)
							.append(' ').append(this.appProps.get(fdadvisorClient1CertBase64)).toString().getBytes());

			resultObj = base64EncodedPayload;
		} else {
			resultObj = new ApiError("NO_NEW_CERT", "No new certificate available.");
		}
		return resultObj;
	}


	public Object loadUsers(String accessTokenInRequest) {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Load users -");

		try {

			// Get group membership of user issuing request. Ensure that user belongs to role-airlinefocal group
			// and one and only one airline group.
			User airlineFocalCurrentUser = getUserInfoFromJwtAccessToken(accessTokenInRequest);

			// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
			List<Group> airlineGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());
			List<Group> roleAirlineFocalGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().equals("role-airlinefocal")).peek(g -> logger.info("Role Group: {}", g)).collect(Collectors.toList());
			if (airlineGroups.size() != 1 || roleAirlineFocalGroups.size() != 1) {
				return new ApiError("USERS_LIST_FAILED", "User membership is ambiguous, airlines[" + airlineGroups.size() + "] roles[" + roleAirlineFocalGroups.size() + "]", RequestFailureReason.UNAUTHORIZED);
			}
			progressLog.append("\nRequesting user belongs to single airline group and airline focal role group");

			// Get access token based on delegated permission via impersonation with a Local Administrator of the tenant.
			String accessToken = null;
			Object authResult = getElevatedPermissionsAccessToken(PermissionType.IMPERSONATION);
			if (authResult instanceof ApiError) {
				return authResult;
			}
			// access token could not be obtained either via delegated permissions or application permissions.
			else if (authResult == null) {
				return new ApiError("USERS_LIST_FAILED", "Failed to acquire permissions", RequestFailureReason.UNAUTHORIZED);
			}
			else {
				accessToken = String.valueOf(authResult);
				progressLog.append("\nObtained impersonation access token");
			}

			// Build URL to get initial users (under 100)
			URL url = new URL(new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
					.append("/groups/").append(airlineGroups.get(0).getObjectId()).append("/members")
					.append('?').append(new StringBuilder(Constants.AZURE_API_VERSION_PREFIX)
							.append(azureadApiVersion).toString())
					.toString());

			HttpsURLConnection connListUsers = (HttpsURLConnection) url.openConnection();
			// Set the appropriate header fields in the request header.
			connListUsers.setRequestMethod("GET");
			connListUsers.setRequestProperty("api-version", azureadApiVersion);
			connListUsers.setRequestProperty("Authorization", accessToken);
			connListUsers.setRequestProperty("Content-Type", "application/json");
			connListUsers.setRequestProperty("Accept", Constants.ACCEPT_CT_JSON_ODATAMINIMAL);

			String responseStr = HttpClientHelper.getResponseStringFromConn(connListUsers, connListUsers.getResponseCode() == HttpStatus.OK.value());
			ObjectMapper mapper = new ObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.setSerializationInclusion(Include.NON_NULL);
			JsonNode errorNode = StringUtils.isBlank(responseStr) ? null : mapper.readTree(responseStr).path("odata.error");

			// Get the nextNode, if available
			JsonNode nextNode = StringUtils.isBlank(responseStr) ? null : mapper.readTree(responseStr).path("odata.nextLink");

			List<User> membersOfAirlineGroup = new ArrayList<>();
			logger.info("List of users in {} group in Azure AD", airlineGroups.get(0).getDisplayName());

			if (connListUsers.getResponseCode() == HttpStatus.OK.value() && (errorNode == null || errorNode instanceof MissingNode)) {

				Iterator<JsonNode> iterator = mapper.readTree(responseStr).path("value").iterator();
				while (iterator.hasNext()) {
					JsonNode nextElemNode = iterator.next();
					if (nextElemNode.path("objectType").asText().equals("User")) {

						User member = mapper.readValue(nextElemNode.toString(), User.class);
						// need to obtain roles for each member
						UserMembership userMembership = getUserMembershipFromGraph(member.getObjectId(), accessToken);
						member.setGroups(userMembership.getUserGroups());
						//member.setDirectoryRoles(userMembership.getUserRoles());
						membersOfAirlineGroup.add(member);
						try {
							userAccountRegister.updateUserAccount(member);
						} catch (UserAccountRegistrationException ex) {
							ex.printStackTrace();
						}
					}
				}

			}

			// Check for nextNode
			while (nextNode != null && nextNode.textValue() != null && !nextNode.textValue().equals(""))
			{
				// Build URL to get next users (next 100)
				url = new URL(new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
						.append('/').append(nextNode.textValue()).toString());

				connListUsers = (HttpsURLConnection) url.openConnection();
				// Set the appropriate header fields in the request header.
				connListUsers.setRequestMethod("GET");
				connListUsers.setRequestProperty("api-version", azureadApiVersion);
				connListUsers.setRequestProperty("Authorization", accessToken);
				connListUsers.setRequestProperty("Content-Type", "application/json");
				connListUsers.setRequestProperty("Accept", Constants.ACCEPT_CT_JSON_ODATAMINIMAL);

				responseStr = HttpClientHelper.getResponseStringFromConn(connListUsers, connListUsers.getResponseCode() == HttpStatus.OK.value());
				mapper = new ObjectMapper()
						.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
						.setSerializationInclusion(Include.NON_NULL);

				errorNode = StringUtils.isBlank(responseStr) ? null : mapper.readTree(responseStr).path("odata.error");

				// Get the nextNode, if available
				nextNode = StringUtils.isBlank(responseStr) ? null : mapper.readTree(responseStr).path("odata.nextLink");
				logger.info("nextNode [{}]", nextNode.textValue());

				if (connListUsers.getResponseCode() == HttpStatus.OK.value() && (errorNode == null || errorNode instanceof MissingNode)) {

					Iterator<JsonNode> iterator = mapper.readTree(responseStr).path("value").iterator();
					while (iterator.hasNext()) {
						JsonNode nextElemNode = iterator.next();
						if (nextElemNode.path("objectType").asText().equals("User")) {

							User member = mapper.readValue(nextElemNode.toString(), User.class);
							// need to obtain roles for each member
							UserMembership userMembership = getUserMembershipFromGraph(member.getObjectId(), accessToken);
							member.setGroups(userMembership.getUserGroups());
							//member.setDirectoryRoles(userMembership.getUserRoles());
							membersOfAirlineGroup.add(member);
							try {
								userAccountRegister.updateUserAccount(member);
							} catch (UserAccountRegistrationException ex) {
								ex.printStackTrace();
							}
						}
					}
				}
			} // end of while nextNode is available

			logger.info("returned [{}] members of Airline Group", membersOfAirlineGroup.size());

			resultObj = mapper.setSerializationInclusion(Include.NON_NULL).setSerializationInclusion(Include.NON_EMPTY)
					.writeValueAsString(membersOfAirlineGroup);
			progressLog.append("\nObtained members of group in Azure AD");

		} catch (JsonProcessingException jpe) {
			logger.error("JsonProcessingException: {}", jpe.getMessage(), jpe);
			resultObj = new ApiError("USERS_LIST_FAILED", "FDAGNDSVCERR0002");
		} catch (MalformedURLException murle) {
			logger.error("MalformedURLException: {}", murle.getMessage(), murle);
			resultObj = new ApiError("USERS_LIST_FAILED", "FDAGNDSVCERR0004");
		} catch (ProtocolException pe) {
			logger.error("ProtocolException: {}", pe.getMessage(), pe);
			resultObj = new ApiError("USERS_LIST_FAILED", "FDAGNDSVCERR0008");
		} catch (IOException ioe) {
			logger.error("IOException: {}", ioe.getMessage(), ioe);
			resultObj = new ApiError("USERS_LIST_FAILED", "FDAGNDSVCERR0016");
		} finally {

			if (resultObj instanceof ApiError) {
				logger.error("FDAGndSvcLog> {}", ControllerUtils.sanitizeString(progressLog.toString()));
			}
		}

		return resultObj;
	}


	public Object deleteUser(String userId, String accessTokenInRequest, boolean isSuperAdmin) {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Delete user -");

		try {

			// Get group membership of user issuing request. Ensure that user belongs to role-airlinefocal group
			 // and one and only one airline group.
			User airlineFocalCurrentUser = getUserInfoFromJwtAccessToken(accessTokenInRequest);
			
			// Ensure requesting user is not trying to delete self!
			if (airlineFocalCurrentUser.getObjectId().equals(userId)) {
				return new ApiError("USER_DELETE_FAILED", "User cannot delete self", RequestFailureReason.BAD_REQUEST);
			}

			// Validate user privileges by checking group membership. User must either:
			// - perform operation in superadmin mode and have User Account Administrator directory role
			// -or-
			// - belong to Role-AirlineFocal group and a single Airline group
			if (isSuperAdmin
				&& (CollectionUtils.isEmpty(airlineFocalCurrentUser.getDirectoryRoles())
					|| !airlineFocalCurrentUser.getDirectoryRoles().stream().anyMatch(g -> g.getDisplayName().toLowerCase().equals("user account administrator")))) {
				return new ApiError("USER_DELETE_FAILED", "User has insufficient privileges", RequestFailureReason.UNAUTHORIZED);
			}

			List<Group> airlineGroups = null,
					    roleAirlineFocalGroups = null;
			if (!isSuperAdmin) {
				
				airlineGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).peek(g -> logger.debug("Airline Group: {}", g)).collect(Collectors.toList());
				roleAirlineFocalGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().equals("role-airlinefocal")).peek(g -> logger.debug("Role Group: {}", g)).collect(Collectors.toList());
				if (airlineGroups.size() != 1 || roleAirlineFocalGroups.size() != 1) {
					return new ApiError("USER_DELETE_FAILED", "User membership is ambiguous", RequestFailureReason.UNAUTHORIZED);
				}
				progressLog.append("\nRequesting user belongs to single airline group and airline focal role group");
			}

			// Get access token based on delegated permission via impersonation with a Local Administrator of the tenant.
			String adminAccessToken = null;
			Object authResult = getElevatedPermissionsAccessToken(PermissionType.IMPERSONATION);
			if (authResult instanceof ApiError) {
				return authResult;
			}
			// The access token could not be obtained either via delegated permissions or application permissions.
			else if (authResult == null) {
				return new ApiError("USER_DELETE_FAILED", "Failed to acquire permissions", RequestFailureReason.UNAUTHORIZED);
			}
			else {
				adminAccessToken = String.valueOf(authResult);
				progressLog.append("\nObtained impersonation access token");
			}

			// Airline focal can delete a user, only if an user matching the user object ID exists
			User deleteUser = getUserInfoFromGraph(userId, adminAccessToken);
			if (deleteUser == null) {
				return new ApiError("USER_DELETE_FAILED", "No user found with matching identifier", RequestFailureReason.NOT_FOUND);
			}
			UserMembership userMembership = getUserMembershipFromGraph(deleteUser.getObjectId(), adminAccessToken);
			List<Group> deleteUserGroups = userMembership.getUserGroups();
			if (deleteUserGroups != null) {

				List<Group> deleteUserAirlineGroups = deleteUserGroups.stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
				// Ensure airline focal is not deleting user in another airline, by getting airline group membership
				// If operation is performed in SuperAdmin mode, skip this check.
				if (!isSuperAdmin
					&& (deleteUserAirlineGroups.size() != 1 || !deleteUserAirlineGroups.get(0).getObjectId().equals(airlineGroups.get(0).getObjectId()))) {
					return new ApiError("USER_DELETE_FAILED", "Not allowed to delete user if not in same airline group", RequestFailureReason.UNAUTHORIZED);
				}
				else {
					// continue
				}
			}
			else {
				return new ApiError("USER_DELETE_FAILED", "Not allowed to delete user if not in same airline group", RequestFailureReason.UNAUTHORIZED);
			}

			progressLog.append(isSuperAdmin ? "\nUser to be deleted by a SuperAdmin" : "User to be deleted is in same airline group");

			// All checks passed. The user can be deleted by invoking the Azure AD Graph API.
			URL url = new URL(new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
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
			
			String responseStr = HttpClientHelper.getResponseStringFromConn(connDeleteUser, connDeleteUser.getResponseCode() == HttpStatus.NO_CONTENT.value());
			ObjectMapper mapper = new ObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.setSerializationInclusion(Include.NON_NULL);
			JsonNode errorNode = StringUtils.isBlank(responseStr) ? null : mapper.readTree(responseStr).path("odata.error");
			if (connDeleteUser.getResponseCode() == HttpStatus.NO_CONTENT.value() && errorNode == null) {
				logger.info("Deleted user {} in Azure AD", deleteUser.getUserPrincipalName());
				progressLog.append("\nDeleted user in Azure AD");
			}

			// Delete any associated records in the User Account Registration database table
			userAccountRegister.removeUserAccountRegistrationData(deleteUser.getUserPrincipalName());
			logger.info("Removed account registration records for user {} in Azure AD", deleteUser.getUserPrincipalName());
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


	public Object enableRepeatableUserRegistration(UserAccountActivation userAccountActivation) throws UserAccountRegistrationException {

		logger.debug("Invoking user registration (possibly repeated user registration)");

		// Validate the password provided by the admin; if invalid, return appropriate error key/description
		if (!PasswordPolicyEnforcer.validate(userAccountActivation.getPassword())) {
			return new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", PasswordPolicyEnforcer.ERROR_PSWD_FAILED_DESCRIPTION, RequestFailureReason.BAD_REQUEST);
		}

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Activate user -");

		HttpsURLConnection connGetUser = null, connEnableUser = null;
		try {

			// Get access token based on delegated permission via impersonation with a Local Administrator of the tenant.
			String adminAccessToken = null;
			Object elevatedAuthResult = getElevatedPermissionsAccessToken(PermissionType.IMPERSONATION);
			if (elevatedAuthResult instanceof ApiError) {
				throw new ElevatedPermissionException(((ApiError) elevatedAuthResult).getErrorDescription());
			}
			// access token could not be obtained either via delegated permissions or application permissions.
			else if (elevatedAuthResult == null) {
				throw new ElevatedPermissionException("Failed to acquire permissions");
			}
			else {
				adminAccessToken = String.valueOf(elevatedAuthResult);
				progressLog.append("\nObtained impersonation access token");
			}

			// - Check database for registration token. If found, proceed to update user account and database
			//   and return registration response to caller.
			// - Registration token not found => try to authenticate user with supplied credentials. If
			//   authentication is successful, return registration response to caller.
			// - Authentication failed => find user account and determine status of account.
			//     * If account is active, indicate to caller than credentials of an existing account are invalid.
			//     * If account is disabled, indicate to caller than registration token is not found

			// Check if token exists and corresponds to PENDING_USER_ACTIVATION in the database
			boolean isUserNotActivated = userAccountRegister.isUserAccountNotActivated(userAccountActivation.getRegistrationToken(), userAccountActivation.getUsername(), Constants.UserAccountState.PENDING_USER_ACTIVATION.toString());
			if (!isUserNotActivated) {

				logger.warn("User registration record/token not found");
				Object ar = getAccessTokenFromUserCredentials(userAccountActivation.getUsername(), userAccountActivation.getPassword());
				if (ar != null && ar instanceof AuthenticationResult) {

					AuthenticationResult authResult = (AuthenticationResult) ar;
					UserMembership userMembership = getUserMembershipFromGraph(authResult.getUserInfo().getUniqueId(), adminAccessToken);
					//  -> Extract airline group
					List<Group> userAirlineGroups = userMembership.getUserGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
					//  -> Extract list of roles
					List<Group> userRoleGroups = userMembership.getUserGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_USER_ROLE_PREFIX)).collect(Collectors.toList());
					String groupName = userAirlineGroups.size() == 1 ? userAirlineGroups.get(0).getDisplayName() : null;
					List<String> roleNames = userRoleGroups.stream().map(g -> g.getDisplayName()).collect(Collectors.toList());
					String airline = groupName != null ? groupName.replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY) : null;

					String getPlistFromSQL = airline != null ? getPlistFromSQL(airline, roleNames.get(0)) : null;

					String mobileConfigFromBlob = airline != null ? getMobileConfigFromBlob("config", new StringBuilder(airline).append(".mobileconfig").toString()) : null;
					if (getPlistFromSQL != null && mobileConfigFromBlob != null) {
						UserRegistration userReg = new UserRegistration(authResult, groupName, roleNames, getPlistFromSQL, mobileConfigFromBlob);
						resultObj = userReg;
					}
					else {
						throw new MobileConfigurationException("Unable to retrieve mobile configuration and/or preferences");
					}
				} else {

					URL url = new URL(new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
							.append("/users/").append(userAccountActivation.getUsername())
							.append('?').append(new StringBuilder(Constants.AZURE_API_VERSION_PREFIX)
									.append(azureadApiVersion).toString())
							.toString());

					connGetUser = (HttpsURLConnection) url.openConnection();
					// Set the appropriate header fields in the request header.
					connGetUser.setRequestMethod(RequestMethod.GET.toString());
					connGetUser.setRequestProperty("api-version", azureadApiVersion);
					connGetUser.setRequestProperty("Authorization", adminAccessToken);
					connGetUser.setRequestProperty("Content-Type", "application/json");
					connGetUser.setUseCaches(false);
					connGetUser.setRequestProperty("Accept", Constants.ACCEPT_CT_JSON_ODATAMINIMAL);

					String responseStr = HttpClientHelper.getResponseStringFromConn(connGetUser, connGetUser.getResponseCode() == HttpStatus.OK.value());
					User userObj = null;
					int responseCode = connGetUser.getResponseCode();
					if (responseCode == 200) {

						ObjectMapper mapper = new ObjectMapper()
								.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
								.setSerializationInclusion(Include.NON_NULL);
						userObj = mapper.readValue(responseStr, User.class);

						progressLog.append("\nRetrieved user object from Azure AD Graph");

						if (Boolean.parseBoolean(userObj.getAccountEnabled())) {
							throw new UserAccountRegistrationException(new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "Invalid username or password", RequestFailureReason.UNAUTHORIZED));
						} else {
							throw new UserAccountRegistrationException(new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "Invalid or expired user registration token", RequestFailureReason.UNAUTHORIZED));
						}
					} else if (responseCode == 404) {

						JsonNode errorNode = new ObjectMapper()
								.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
								.setSerializationInclusion(Include.NON_NULL).readTree(responseStr).path("odata.error");
						JsonNode messageNode = errorNode.path("message");
						JsonNode valueNode = messageNode.path("value");
						logger.warn("Failed to register/authenticate user: {}", valueNode.asText());
						throw new UserAccountRegistrationException(new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "Invalid username or password", RequestFailureReason.UNAUTHORIZED));
					} else {

						JsonNode errorNode = new ObjectMapper()
								.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
								.setSerializationInclusion(Include.NON_NULL).readTree(responseStr).path("odata.error");
						JsonNode messageNode = errorNode.path("message");
						JsonNode valueNode = messageNode.path("value");
						logger.warn("Error retrieving user: {}", valueNode.asText());
						throw new UserAccountRegistrationException(new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "Failed to associate user with registration request", RequestFailureReason.INTERNAL_SERVER_ERROR));
					}
				}
			} else { // User registration record is found

				progressLog.append("\nUser account registration record/token found");

				URL url = new URL(new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
						.append("/users/").append(userAccountActivation.getUsername())
						.append('?').append(new StringBuilder(Constants.AZURE_API_VERSION_PREFIX)
								.append(azureadApiVersion).toString())
						.toString());

				connGetUser = (HttpsURLConnection) url.openConnection();
				// Set the appropriate header fields in the request header.
				connGetUser.setRequestMethod(RequestMethod.GET.toString());
				connGetUser.setRequestProperty("api-version", azureadApiVersion);
				connGetUser.setRequestProperty("Authorization", adminAccessToken);
				connGetUser.setRequestProperty("Content-Type", "application/json");
				connGetUser.setRequestProperty("Accept", Constants.ACCEPT_CT_JSON_ODATAMINIMAL);

				String responseStr = HttpClientHelper.getResponseStringFromConn(connGetUser, connGetUser.getResponseCode() == HttpStatus.OK.value());
				User userObj = null;
				if (connGetUser.getResponseCode() == 200) {

					ObjectMapper mapper = new ObjectMapper()
							.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
							.setSerializationInclusion(Include.NON_NULL);
					userObj = mapper.readValue(responseStr, User.class);

					if (Boolean.parseBoolean(userObj.getAccountEnabled())) {
						throw new UserAccountRegistrationException(new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "User account is already activated", RequestFailureReason.CONFLICT));
					}
					progressLog.append("\nRetrieved user object from Azure AD Graph");
				}

				// Send PATCH request via impersonation to Azure AD Graph, to enable account and set password
				// Build a payload for enabling user account and setting the password
				ObjectMapper mapper = new ObjectMapper();
				ObjectNode rootNode = mapper.createObjectNode();
				rootNode.put("accountEnabled", true);
				ObjectNode pwdProfileNode = mapper.createObjectNode();
				pwdProfileNode.put("password", userAccountActivation.getPassword());
				pwdProfileNode.put("forceChangePasswordNextLogin", false);
				rootNode.set("passwordProfile", pwdProfileNode);

				url = new URL(new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
						.append("/users/").append(userAccountActivation.getUsername())
						.append('?').append(new StringBuilder(Constants.AZURE_API_VERSION_PREFIX)
								.append(azureadApiVersion).toString())
						.toString());

				connEnableUser = (HttpsURLConnection) url.openConnection();
				// Set the appropriate header fields in the request header.
				connEnableUser.setRequestMethod(RequestMethod.POST.toString());
				connEnableUser.setRequestProperty("X-HTTP-Method-Override", RequestMethod.PATCH.toString());
				connEnableUser.setRequestProperty("api-version", azureadApiVersion);
				connEnableUser.setRequestProperty("Authorization", adminAccessToken);
				connEnableUser.setRequestProperty("Content-Type", "application/json");
				connEnableUser.setRequestProperty("Accept", Constants.ACCEPT_CT_JSON_ODATAMINIMAL);
				connEnableUser.setDoOutput(true);
				try (DataOutputStream dos = new DataOutputStream(connEnableUser.getOutputStream())) {
					dos.writeBytes(mapper.writeValueAsString(rootNode));
 					dos.flush();
				}

				responseStr = HttpClientHelper.getResponseStringFromConn(connEnableUser, connEnableUser.getResponseCode() == HttpStatus.NO_CONTENT.value());
				JsonNode errorNode = StringUtils.isBlank(responseStr) ? null : mapper.readTree(responseStr).path("odata.error");
				if (connEnableUser.getResponseCode() == HttpStatus.NO_CONTENT.value() && errorNode == null) {
					logger.debug("Enabled user and set password");
					progressLog.append("\nEnabled user and set password");
				}
				else {

					JsonNode messageNode = errorNode.path("message");
					JsonNode valueNode = messageNode.path("value");
					logger.error("Failed to enable user and set password: {}", valueNode.asText());
					throw new UserAccountRegistrationException(new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "Failed to enable user and set password", RequestFailureReason.INTERNAL_SERVER_ERROR));
				}

				// Update UserAccountRegistrations database table to invalidate token (by updating account state)
				userAccountRegister.enableNewUserAccount(userAccountActivation.getRegistrationToken(), userObj.getUserPrincipalName(), Constants.UserAccountState.PENDING_USER_ACTIVATION.toString(), Constants.UserAccountState.USER_ACTIVATED.toString());
				progressLog.append("\nActivated new user account in database");

				Object ar = getAccessTokenFromUserCredentials(userObj.getUserPrincipalName(), userAccountActivation.getPassword());
				if (ar == null) {
					throw new UserAccountRegistrationException(new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "Failed to obtain auth token for user credentials", RequestFailureReason.UNAUTHORIZED));
				}

				if (ar instanceof AuthenticationResult) {

					AuthenticationResult authResult = (AuthenticationResult) ar;
					// Get Airline and list of roles from authentication result which encapsulates the access token and user object ID
					// Note: Use impersonated user's (with elevated permissions) access token to get the user group membership since
					//       the user account was just now enabled and the AAD directory replicas may still be synchronizing with the change.
					UserMembership userMembership = getUserMembershipFromGraph(authResult.getUserInfo().getUniqueId(), adminAccessToken);
					//  -> Extract airline group
					List<Group> userAirlineGroups = userMembership.getUserGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
					//  -> Extract list of roles
					List<Group> userRoleGroups = userMembership.getUserGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_USER_ROLE_PREFIX)).collect(Collectors.toList());
					String groupName = userAirlineGroups.size() == 1 ? userAirlineGroups.get(0).getDisplayName() : null;
					List<String> roleNames = userRoleGroups.stream().map(g -> g.getDisplayName()).collect(Collectors.toList());
					String airline = groupName != null ? groupName.replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY) : null;

					String getPlistFromSQL = airline != null ? getPlistFromSQL(airline, roleNames.get(0)) : null;

					String mobileConfigFromBlob = airline != null ? getMobileConfigFromBlob("config", new StringBuilder(airline).append(".mobileconfig").toString()) : null;
					if (getPlistFromSQL != null && mobileConfigFromBlob != null) {
						UserRegistration userReg = new UserRegistration(authResult, groupName, roleNames, getPlistFromSQL, mobileConfigFromBlob);
						resultObj = userReg;
					}
					else {
						throw new MobileConfigurationException("Unable to retrieve mobile configuration and/or preferences");
					}
				}

				if (resultObj == null && ExceptionUtils.indexOfThrowable((Throwable) ar, AuthenticationException.class) >= 0) {
					//resultObj = AzureADClientHelper.getLoginErrorFromString(((AuthenticationException) ar).getMessage());
					throw new AuthenticationException(((AuthenticationException) ar).getMessage());
				}
			}
		} catch (AuthenticationException ae) {
			logger.error("Failed to activate user account: {}", ae.getMessage());
			resultObj = new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "FDAGNDSVCERR0002");
		} catch (MobileConfigurationException mce) {
			logger.error("Failed to activate user account: {}", mce.getMessage());
			resultObj = new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", mce.getMessage());
		} catch (ElevatedPermissionException epe) {
			logger.error("Failed to activate user account: {}", epe.getMessage());
			resultObj = new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "FDAGNDSVCERR0004");
		} catch (MalformedURLException murle) {
			logger.error("MalformedURLException: {}", murle.getMessage(), murle);
			resultObj = new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "FDAGNDSVCERR0008");
		} catch (ProtocolException pe) {
			logger.error("ProtocolException: {}", pe.getMessage(), pe);
			resultObj = new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "FDAGNDSVCERR0016");
		} catch (IOException ioe) {
			logger.error("IOException: {}", ioe.getMessage(), ioe);
			resultObj = new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "FDAGNDSVCERR0032");
		} finally {

			if (resultObj instanceof ApiError) {
				logger.error("FDAGndSvcLog> {}", ControllerUtils.sanitizeString(progressLog.toString()));
			}
		}

		return resultObj;
	}

	public Object getClientCertFromActivationCode(String email, String code) throws UserAccountRegistrationException {
		logger.debug("Validate one-time activation code [{}] for [{}]", code, email);
		// First validate activation code
		List<ActivationCode> codes = userAccountRegister.getActivationCode(email, code);
		if (codes.isEmpty() || codes.size() > 1) {
			return new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE",
					"Missing or Invalid Activation Code, Missing or Invalid Email Address", RequestFailureReason.BAD_REQUEST);
		}

		// Valid code, get activation information
		ActivationCode activationCode = codes.get(0);
		logger.debug("[{}] is a Valid code for [{}]", code, email);
		// Remove code
		List<String> persistentEmailFromBlob = getPersistentEmailFromBlob("persistent-emails", new StringBuilder("persistent.emails").toString());

		// If not a persistent email, then remove it.
		if (!persistentEmailFromBlob.contains(email.toLowerCase())) {
			logger.debug("Remove activation code [{}] for [{}]", code, email);
			userAccountRegister.removeActivationCode(email, code);
		} else {
			logger.debug("Retain persistent email -> [{}]", email);
		}
		Object resultObj = activationCode;
		return resultObj;
	}

	public Object enableUserAndSetPassword(UserAccountActivation userAccountActivation) throws UserAccountRegistrationException {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Activate user -");

		HttpsURLConnection connGetUser = null, connEnableUser = null;
		try {

			// Check if token exists and corresponds to PENDING_USER_ACTIVATION in the database
			boolean isUserNotActivated = userAccountRegister.isUserAccountNotActivated(userAccountActivation.getRegistrationToken(), userAccountActivation.getUsername(), Constants.UserAccountState.PENDING_USER_ACTIVATION.toString());
			if (!isUserNotActivated) {
				logger.warn("User registration record/token not found");
				// If user does not exist, or user account is disabled, throw exception.
				throw new UserAccountRegistrationException(new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "Invalid or expired user account activation token", RequestFailureReason.NOT_FOUND));
				// Else, user is trying to register again, perhaps on an other device, so proceed
				// without needing to update the database, and just return the preferences, config and user role.
			}
			progressLog.append("\nUser account registration record/token found");

			// Check if user corresponding to userPrincipalName exists in Azure AD and is currently not enabled.
			// Get access token based on delegated permission via impersonation with a Local Administrator of the tenant.
			String accessToken = null;
			Object elevatedAuthResult = getElevatedPermissionsAccessToken(PermissionType.IMPERSONATION);
			if (elevatedAuthResult instanceof ApiError) {
				throw new ElevatedPermissionException(((ApiError) elevatedAuthResult).getErrorDescription());
			}
			// access token could not be obtained either via delegated permissions or application permissions.
			else if (elevatedAuthResult == null) {
				throw new ElevatedPermissionException("Failed to acquire permissions");
			}
			else {
				accessToken = String.valueOf(elevatedAuthResult);
				progressLog.append("\nObtained impersonation access token");
			}

			URL url = new URL(new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
					.append("/users/").append(userAccountActivation.getUsername())
					.append('?').append(new StringBuilder(Constants.AZURE_API_VERSION_PREFIX)
							.append(azureadApiVersion).toString())
					.toString());

			connGetUser = (HttpsURLConnection) url.openConnection();
			// Set the appropriate header fields in the request header.
			connGetUser.setRequestMethod(RequestMethod.GET.toString());
			connGetUser.setRequestProperty("api-version", azureadApiVersion);
			connGetUser.setRequestProperty("Authorization", accessToken);
			connGetUser.setRequestProperty("Content-Type", "application/json");
			connGetUser.setRequestProperty("Accept", Constants.ACCEPT_CT_JSON_ODATAMINIMAL);

			String responseStr = HttpClientHelper.getResponseStringFromConn(connGetUser, connGetUser.getResponseCode() == HttpStatus.OK.value());
			User userObj = null;
			if (connGetUser.getResponseCode() == 200) {

				ObjectMapper mapper = new ObjectMapper()
						.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
						.setSerializationInclusion(Include.NON_NULL);
				userObj = mapper.readValue(responseStr, User.class);
				
				if (Boolean.parseBoolean(userObj.getAccountEnabled())) {
					throw new UserAccountRegistrationException(new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "User account is already activated", RequestFailureReason.CONFLICT));
				}
				progressLog.append("\nRetrieved user object from Azure AD Graph");
			}

			// Send PATCH request via impersonation to Azure AD Graph, to enable account and set password
			// Build a payload for enabling user account and setting the password
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode rootNode = mapper.createObjectNode();
			rootNode.put("accountEnabled", true);
			ObjectNode pwdProfileNode = mapper.createObjectNode();
			pwdProfileNode.put("password", userAccountActivation.getPassword());
			pwdProfileNode.put("forceChangePasswordNextLogin", false);
			rootNode.set("passwordProfile", pwdProfileNode);

			url = new URL(new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
					.append("/users/").append(userAccountActivation.getUsername())
					.append('?').append(new StringBuilder(Constants.AZURE_API_VERSION_PREFIX)
							.append(azureadApiVersion).toString())
					.toString());

			connEnableUser = (HttpsURLConnection) url.openConnection();
			// Set the appropriate header fields in the request header.
			connEnableUser.setRequestMethod(RequestMethod.POST.toString());
			connEnableUser.setRequestProperty("X-HTTP-Method-Override", RequestMethod.PATCH.toString());
			connEnableUser.setRequestProperty("api-version", azureadApiVersion);
			connEnableUser.setRequestProperty("Authorization", accessToken);
			connEnableUser.setRequestProperty("Content-Type", "application/json");
			connEnableUser.setRequestProperty("Accept", Constants.ACCEPT_CT_JSON_ODATAMINIMAL);
			connEnableUser.setDoOutput(true);
			try (DataOutputStream dos = new DataOutputStream(connEnableUser.getOutputStream())) {
				dos.writeBytes(mapper.writeValueAsString(rootNode));
				dos.flush();
			}
			
			responseStr = HttpClientHelper.getResponseStringFromConn(connEnableUser, connEnableUser.getResponseCode() == HttpStatus.NO_CONTENT.value());
			JsonNode errorNode = StringUtils.isBlank(responseStr) ? null : mapper.readTree(responseStr).path("odata.error");
			if (connEnableUser.getResponseCode() == HttpStatus.NO_CONTENT.value() && errorNode == null) {
				logger.debug("Enabled user and set password");
				progressLog.append("\nEnabled user and set password");
			}
			else {

				JsonNode messageNode = errorNode.path("message");
				JsonNode valueNode = messageNode.path("value");
				logger.error("Failed to enable user and set password: {}", valueNode.asText());
				throw new UserAccountRegistrationException(new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "Failed to enable user and set password", RequestFailureReason.INTERNAL_SERVER_ERROR));
			}

			// Update UserAccountRegistrations database table to invalidate token (by updating account state)
			userAccountRegister.enableNewUserAccount(userAccountActivation.getRegistrationToken(), userObj.getUserPrincipalName(), Constants.UserAccountState.PENDING_USER_ACTIVATION.toString(), Constants.UserAccountState.USER_ACTIVATED.toString());
			progressLog.append("\nActivated new user account in database");

			Object ar = getAccessTokenFromUserCredentials(userObj.getUserPrincipalName(), userAccountActivation.getPassword());
			if (ar == null) {
				throw new UserAccountRegistrationException(new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "Failed to obtain auth token for user credentials", RequestFailureReason.UNAUTHORIZED));
			}
			
			if (ar instanceof AuthenticationResult) {

				AuthenticationResult authResult = (AuthenticationResult) ar;
				// Get Airline and list of roles from authentication result which encapsulates the access token and user object ID
				// Note: Use impersonated user's (with elevated permissions) access token to get the user group membership since
				//       the user account was just now enabled and the AAD directory replicas may still be synchronizing with the change.
				UserMembership userMembership = getUserMembershipFromGraph(authResult.getUserInfo().getUniqueId(), accessToken);
				//  -> Extract airline group
				List<Group> userAirlineGroups = userMembership.getUserGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
				//  -> Extract list of roles
				List<Group> userRoleGroups = userMembership.getUserGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_USER_ROLE_PREFIX)).collect(Collectors.toList());
				String groupName = userAirlineGroups.size() == 1 ? userAirlineGroups.get(0).getDisplayName() : null;
				List<String> roleNames = userRoleGroups.stream().map(g -> g.getDisplayName()).collect(Collectors.toList());
				String airline = groupName != null ? groupName.replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY) : null;

				String getPlistFromSQL = airline != null ? getPlistFromSQL(airline, roleNames.get(0)) : null;

				String mobileConfigFromBlob = airline != null ? getMobileConfigFromBlob("config", new StringBuilder(airline).append(".mobileconfig").toString()) : null;

				if (getPlistFromSQL != null && mobileConfigFromBlob != null) {
					UserRegistration userReg = new UserRegistration(authResult, groupName, roleNames, getPlistFromSQL, mobileConfigFromBlob);
					resultObj = userReg;
				}
				else {
					throw new MobileConfigurationException("Failed to retrieve plist and/or mobile configuration");
				}
			}

			if (resultObj == null && ExceptionUtils.indexOfThrowable((Throwable) ar, AuthenticationException.class) >= 0) {
				//resultObj = AzureADClientHelper.getLoginErrorFromString(((AuthenticationException) ar).getMessage());
				throw new AuthenticationException(((AuthenticationException) ar).getMessage());
			}
		} catch (AuthenticationException ae) {
			logger.error("Failed to activate user account: {}", ae.getMessage());
			resultObj = new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "FDAGNDSVCERR0002");
		} catch (MobileConfigurationException mce) {
			logger.error("Failed to activate user account: {}", mce.getMessage());
			resultObj = new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "FDAGNDSVCERR0001");
		} catch (ElevatedPermissionException epe) {
			logger.error("Failed to activate user account: {}", epe.getMessage());
			resultObj = new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "FDAGNDSVCERR0004");
		} catch (MalformedURLException murle) {
			logger.error("MalformedURLException: {}", murle.getMessage(), murle);
			resultObj = new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "FDAGNDSVCERR0008");
		} catch (ProtocolException pe) {
			logger.error("ProtocolException: {}", pe.getMessage(), pe);
			resultObj = new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "FDAGNDSVCERR0016");
		} catch (IOException ioe) {
			logger.error("IOException: {}", ioe.getMessage(), ioe);
			resultObj = new ApiError("ACTIVATE_USER_ACCOUNT_FAILURE", "FDAGNDSVCERR0032");
		} finally {
			
			if (resultObj instanceof ApiError) {
				logger.error("FDAGndSvcLog> {}", ControllerUtils.sanitizeString(progressLog.toString()));
			}
		}

		return resultObj;
	}

	private User getUserFromGraph(String username, String adminAccessToken, StringBuilder progressLog, String contextKey)
		throws MalformedURLException, IOException, ApiErrorException {

		HttpsURLConnection connGetUser = null;

		User userObj = null;
		
		URL url = new URL(new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
				.append("/users/").append(username)
				.append('?').append(new StringBuilder(Constants.AZURE_API_VERSION_PREFIX)
						.append(azureadApiVersion).toString())
				.toString());

		connGetUser = (HttpsURLConnection) url.openConnection();
		// Set the appropriate header fields in the request header.
		connGetUser.setRequestMethod(RequestMethod.GET.toString());
		connGetUser.setRequestProperty("api-version", azureadApiVersion);
		connGetUser.setRequestProperty("Authorization", adminAccessToken);
		connGetUser.setRequestProperty("Content-Type", "application/json");
		connGetUser.setUseCaches(false);
		connGetUser.setRequestProperty("Accept", Constants.ACCEPT_CT_JSON_ODATAMINIMAL);

		String responseStr = HttpClientHelper.getResponseStringFromConn(connGetUser, connGetUser.getResponseCode() == HttpStatus.OK.value());
		int responseCode = connGetUser.getResponseCode();
		if (responseCode == 200) {

			ObjectMapper mapper = new ObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.setSerializationInclusion(Include.NON_NULL);
			userObj = mapper.readValue(responseStr, User.class);
			progressLog.append("\nRetrieved user object from Azure AD Graph");
		} else if (responseCode == 404) {
			
			JsonNode errorNode = new ObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.setSerializationInclusion(Include.NON_NULL).readTree(responseStr).path("odata.error");
			JsonNode messageNode = errorNode.path("message");
			JsonNode valueNode = messageNode.path("value");
			logger.warn("Failed to retrieve user (404): {}", valueNode.asText());
			progressLog.append("\nFailed to retrieve user (404) from Azure AD Graph");
			throw new ApiErrorException(contextKey, new ApiError(contextKey, "User not found", RequestFailureReason.NOT_FOUND));
		} else {

			JsonNode errorNode = new ObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.setSerializationInclusion(Include.NON_NULL).readTree(responseStr).path("odata.error");
			JsonNode messageNode = errorNode.path("message");
			JsonNode valueNode = messageNode.path("value");
			logger.warn("Failed to retrieve user ({}): {}", responseCode, valueNode.asText());
			progressLog.append("\nFailed to retrieve user (404) from Azure AD Graph");
			throw new ApiErrorException(contextKey, new ApiError(contextKey, "Failed to retrieve user", ControllerUtils.translateHttpErrorCodeToRequestFailureReason(responseCode)));
		}
		
		return userObj;
	}

	public boolean addUserToGroup(String groupObjectId, String userObjectId, String applicationToken) {

		boolean userAddedToGroup = false;

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode rootNode = mapper.createObjectNode();
		rootNode.put("url", new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
				.append("/directoryObjects/").append(userObjectId)
				.toString());

		try {

			URL url = new URL(new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
					.append("/groups/").append(groupObjectId).append("/$links/members")
					.append('?').append(new StringBuilder(Constants.AZURE_API_VERSION_PREFIX)
							.append(azureadApiVersion).toString())
					.toString());

			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			// Set the appropriate header fields in the request header.
			conn.setRequestMethod("POST");
			conn.setRequestProperty("api-version", azureadApiVersion);
			conn.setRequestProperty("Authorization", applicationToken);
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Accept", Constants.ACCEPT_CT_JSON_ODATAMINIMAL);
			conn.setDoOutput(true);
			try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
				dos.writeBytes(mapper.writeValueAsString(rootNode));
				dos.flush();
			}

			// User added to group if HTTP response code is 204 / No Content
			userAddedToGroup = conn.getResponseCode() == HttpStatus.NO_CONTENT.value();
		}
		catch (Exception e) {
			logger.error("Failed to query groups: {}", e.getMessage(), e);
		}

		return userAddedToGroup;
	}

	public User getUserInfoFromJwtAccessToken(String accessToken) {

		logger.debug("Access token for logged in user: {}", ControllerUtils.sanitizeString(accessToken));
		
		String uniqueId = AzureADClientHelper.getUniqueIdFromJWT(accessToken);
		User user = getUserInfoFromGraph(uniqueId, accessToken);
		UserMembership userMembership = getUserMembershipFromGraph(uniqueId, accessToken);
		user.setGroups(userMembership.getUserGroups());
		user.setDirectoryRoles(userMembership.getUserRoles());

		return user;
	}

	private User getUserInfoFromGraph(String uniqueId, String accessToken) {

		logger.debug("Getting user object info from graph");
		User userObj = null;
		try {

			URL url = new URL(new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
					.append("/users/").append(uniqueId)
					.append('?').append(new StringBuilder(Constants.AZURE_API_VERSION_PREFIX)
							.append(azureadApiVersion).toString())
					.toString());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			// Set the appropriate header fields in the request header.
			conn.setRequestProperty("api-version", azureadApiVersion);
			conn.setRequestProperty("Authorization", accessToken);
			conn.setRequestProperty("Accept", Constants.ACCEPT_CT_JSON_ODATAMINIMAL);

			String responseStr = HttpClientHelper.getResponseStringFromConn(conn, true);

			logger.debug("User response from graph API: {}", responseStr);
			
			int responseCode = conn.getResponseCode();

			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			ObjectReader objectReader = objectMapper.readerFor(User.class);
			if (responseCode == 200) {
				userObj = objectReader.readValue(responseStr);
			}
		} catch (MalformedURLException e) {
			this.logger.error("MalformedURLException while retrieving user names from graph: {}", e.getMessage(), e);
		} catch (IOException e) {
			this.logger.error("I/O Exception while retrieving user names from graph: {}", e.getMessage(), e);
		}

		return userObj;
	}



	public UserMembership getUserMembershipFromGraph(String uniqueId, String accessToken) {

		UserMembership userMembership = null;
		HttpURLConnection conn = null;
		try {

			URL url = new URL(new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
					.append("/users/").append(uniqueId).append("/memberOf")
					.append('?').append(new StringBuilder(Constants.AZURE_API_VERSION_PREFIX)
							.append(azureadApiVersion).toString())
					.toString());
			conn = (HttpURLConnection) url.openConnection();
			// Set the appropriate header fields in the request header.
			conn.setRequestProperty("api-version", azureadApiVersion);
			conn.setRequestProperty("Authorization", accessToken);
			conn.setRequestProperty("Accept", Constants.ACCEPT_CT_JSON_ODATAMINIMAL);
			String responseStr = HttpClientHelper.getResponseStringFromConn(conn, true);
			int responseCode = conn.getResponseCode();
			if (responseCode == 200) {
				List<Group> groups = extractUserGroupMembershipFromResponse(responseStr);
				List<DirectoryRole> roles = extractDirectoryRoleMembershipFromResponse(responseStr, true);
				userMembership = new UserMembership(groups, roles);
			}
		} catch (MalformedURLException e) {
			logger.error("MalformedURLException while retrieving groups from graph: {}", e.getMessage(), e);
		} catch (IOException e) {
			logger.error("I/O Exception while retrieving groups from graph: {}", e.getMessage(), e);
		}

		return userMembership;
	}

	public String getUserInfoFromMSGraph(String accessToken) throws IOException {

		URL url = new URL(new StringBuilder(msgraphApiUri).append("/me").toString());
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setRequestMethod("GET");
		conn.setRequestProperty("Authorization", new StringBuilder(Constants.AUTH_HEADER_PREFIX).append(accessToken).toString());
		conn.setRequestProperty("Accept", "application/json");

		int httpResponseCode = conn.getResponseCode();
		if (httpResponseCode == 200) {

			StringBuilder response = new StringBuilder();
			try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {

				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
			} catch (Exception e) {
				logger.error("Failed to read response: {}", e.getMessage());
			}
			return response.toString();
		} else {
			return String.format("Connection returned HTTP code: %s with message: %s", httpResponseCode,
					conn.getResponseMessage());
		}
	}
	
	private List<Group> extractUserGroupMembershipFromResponse(String responseStr) {

		List<Group> groupList = new ArrayList<>();
		try {

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			JsonNode arrNode = mapper.readTree(responseStr).get("value");
			if (arrNode.isArray()) {

				for (JsonNode objNode : arrNode) {

					DirectoryObject dirObj = mapper.treeToValue(objNode, DirectoryObject.class);
					Group group = null;
					if (dirObj.getObjectType().equals("Group")) {
						group = mapper.treeToValue(objNode, Group.class);
						groupList.add(group);
					}
				}
			}
		} catch (IOException e) {
			logger.error("I/O Exception while reading groups from list: {}", e.getMessage(), e);
		}

		return groupList;
	}

	private List<DirectoryRole> extractDirectoryRoleMembershipFromResponse(String responseStr, boolean filterSystemRole) {

		List<DirectoryRole> roleList = new ArrayList<>();
		try {

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			JsonNode arrNode = mapper.readTree(responseStr).get("value");
			if (arrNode.isArray()) {

				for (JsonNode objNode : arrNode) {

					DirectoryObject dirObj = mapper.treeToValue(objNode, DirectoryObject.class);
					DirectoryRole role = null;
					if (dirObj.getObjectType().equals("Role")) {
						role = mapper.treeToValue(objNode, DirectoryRole.class);
						if (!role.isRoleDisabled()
							&& ((filterSystemRole && role.isIsSystem())
								|| (!filterSystemRole && !role.isIsSystem()))) {

							roleList.add(role);
						}
					}
				}
			}
		} catch (IOException e) {
			logger.error("I/O Exception while reading groups from list: {}", e.getMessage(), e);
		}

		return roleList;
	}

	public ObjectNode convertNewUserObjectToJsonPayload(ObjectMapper mapper, NewUser newUserPayload) {
		
		ObjectNode rootNode = mapper.createObjectNode();
		if (newUserPayload.getUserPrincipalName().indexOf('@') < 0) {
			rootNode.put("userPrincipalName", new StringBuilder(newUserPayload.getUserPrincipalName()).append('@').append(this.appProps.get("AzureADCustomTenantName")).toString());
			rootNode.put("mailNickname", newUserPayload.getUserPrincipalName());
		}
		else {
			rootNode.put("userPrincipalName", newUserPayload.getUserPrincipalName());
			rootNode.put("mailNickname", newUserPayload.getUserPrincipalName().substring(0, newUserPayload.getUserPrincipalName().indexOf('@')));
		}
		rootNode.put("accountEnabled", false);
		rootNode.put("givenName", newUserPayload.getGivenName());
		rootNode.put("surname", newUserPayload.getSurname());
		rootNode.put("displayName", newUserPayload.getDisplayName());
		ObjectNode pwdProfileNode = mapper.createObjectNode();
		pwdProfileNode.put("password", newUserPayload.getPassword());
		pwdProfileNode.put("forceChangePasswordNextLogin", newUserPayload.isForceChangePasswordNextLogin());
		rootNode.set("passwordProfile", pwdProfileNode);
		ArrayNode otherMailsNode = mapper.createArrayNode();
		newUserPayload.getOtherMails().stream().forEach(otherMailsNode::add);
		rootNode.set("otherMails", otherMailsNode);
		
		return rootNode;
	}


	private String composeNewUserAccountActivationEmail(User newlyCreatedUser, String registrationToken, boolean newRegistrationProcess)
		throws UserAccountRegistrationException
	{
		if(newRegistrationProcess){
			return emailService.getNewActivationEmailForUser(newlyCreatedUser, registrationToken);
		}else{
			return emailService.getOldActivationEmailForUser(newlyCreatedUser, registrationToken);
		}
	}

	public File getFileFromBlob(String containerName, String fileName, String airline) throws FileDownloadException {

		File file = null;
		try {

			AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"),
				this.appProps.get("StorageKey"));

			file = asu.downloadBlobReferencedInMessage(containerName, fileName, airline);

		} catch (IOException | StorageException | URISyntaxException ioe ) {
			logger.error("Failed to retrieve Blob from storage [{}]: {}", fileName, ioe.getMessage(), ioe);
			throw new FileDownloadException();
		}
		return file;
	}

	private String getPlistFromSQL(String airline, String userRole) {

		StringBuilder preferencesBody = new StringBuilder();

		try {
			if (!airline.startsWith("airline-")) {
				airline = "airline-" + airline;
			}

			List<AirlinePreferences> airlinePreferences = featureManagementDao.getAirlinePreferences(airline, false);
			preferencesBody.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
			preferencesBody.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\r\n");
			preferencesBody.append("<plist version=\"1.0\">\r\n").append("<dict>\r\n");
			preferencesBody.append("<key>AIDProtocolKey</key>\r\n");
			preferencesBody.append("<string>https</string>\r\n");
			preferencesBody.append("<key>AIDHostKey</key>\r\n");
			preferencesBody.append("<string>172.31.1.1</string>\r\n");
			for (AirlinePreferences ap : airlinePreferences)
			{
				preferencesBody.append("<key>" + ap.getAirlineKey() + "</key>\r\n");
				if (userRole.equals("role-airlinefocal")) {
					if (ap.isChoiceFocal()) {
						preferencesBody.append("<true/>\r\n");
					} else {
						preferencesBody.append("<false/>\r\n");
					}
				}
				if (userRole.equals("role-airlinepilot")) {
					if (ap.isChoicePilot()) {
						preferencesBody.append("<true/>\r\n");
					} else {
						preferencesBody.append("<false/>\r\n");
					}
				}
				if (userRole.equals("role-airlinecheckairman")) {
					if (ap.isChoiceCheckAirman()) {
						preferencesBody.append("<true/>\r\n");
					} else {
						preferencesBody.append("<false/>\r\n");
					}
				}
				if (userRole.equals("role-airlinemaintenance")) {
					if (ap.isChoiceMaintenance()) {
						preferencesBody.append("<true/>\r\n");
					} else {
						preferencesBody.append("<false/>\r\n");
					}
				}
				if (userRole.equals("role-airlineefbadmin")) {
					if (ap.isChoiceMaintenance()) {
						preferencesBody.append("<true/>\r\n");
					} else {
						preferencesBody.append("<false/>\r\n");
					}
				}
			}
			List<UserPreferences> userPreferences = featureManagementDao.getUserPreferences(airline);
			for (UserPreferences up : userPreferences) {
				preferencesBody.append("<key>" + up.getUserKey() + "</key>\r\n");
				if (!up.isToggle()) {
					preferencesBody.append("<string>" + up.getValue() + "</string>\r\n");
				} else {
					preferencesBody.append("<"+ up.getValue() +"/>\r\n");
				}
			}
			preferencesBody.append("</dict>\r\n").append("</plist>\r\n");

			logger.info("returned [{}] airline preferences records of Airline Group [{}]", airlinePreferences.size(), airline);

		} catch (FeatureManagementException fme) {
			logger.error("FeatureManagementException: {}", fme.getMessage(), fme);
		}
		return preferencesBody.toString();
	}


	private String getMobileConfigFromBlob(String containerName, String fileName) {

        String base64 = null;
        try {
            AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"), this.appProps.get("StorageKey"));
            try (ByteArrayOutputStream outputStream = asu.downloadFile(containerName, fileName)) {

				base64 = Base64.getEncoder().encodeToString(outputStream.toString().getBytes());

            } catch (NullPointerException npe) {
            	logger.error("Failed to retrieve Plist [{}]: {}", fileName, npe.getMessage(), npe);
            }
        }
        catch (IOException ioe) {
            logger.error("Failed to retrieve Plist [{}]: {}", fileName, ioe.getMessage(), ioe);
        }

        return base64;
    }

	private List<String> getPersistentEmailFromBlob(String containerName, String fileName) {

		List<String> emails = new ArrayList<>();
		try {
			AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"), this.appProps.get("StorageKey"));
			try (ByteArrayOutputStream outputStream = asu.downloadFile(containerName, fileName)) {
				String base = outputStream.toString().toLowerCase();
				logger.debug("{} contains [{}]", fileName, base);
				emails = Arrays.asList(base.split(";"));
				logger.debug("{} contains [{}] emails", fileName, emails.size());

			} catch (NullPointerException npe) {
				logger.error("Failed to retrieve Persistent emails [{}]: {}", fileName, npe.getMessage(), npe);
			}
		}
		catch (IOException ioe) {
			logger.error("Failed to retrieve Persistent emails [{}]: {}", fileName, ioe.getMessage(), ioe);
		}
		logger.info("found [{}] persistent emails.", emails.size());
		if (!emails.isEmpty())
			for (String email : emails)
				logger.debug("entry [{}]", email);
		else {
			logger.debug("{} in {} is empty.", fileName, containerName);
		}
		return emails;
	}

	public ApiError getLoginErrorFromString(String responseString) {

		ApiError errorObj = null;

		try {

			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> map = mapper.readValue(responseString, new TypeReference<HashMap<String, String>>() {
			});
			logger.error("error response: {}", responseString);
			String errorDesc = map.get("error_description");
			String errorString = errorDesc.split("\\r?\\n")[0];
			errorObj = new ApiError(map.get("error"), errorString);
		} catch (IOException ioe) {
			logger.error("Failed to extract login error from string: {}", ioe.getMessage(), ioe);
			errorObj = new ApiError("Authentication failed", ioe.getMessage());
		}

		return errorObj;
	}


	public boolean uploadAttachment(final File uploadfile) throws IOException {

		logger.debug("Upload File method invoked -  Single file upload!");

		String uploadFolder = null;
		try {
			uploadFolder = this.saveUploadedFile(uploadfile);
			if (StringUtils.isBlank(uploadFolder)) {
				throw new IOException("Failed to establish upload folder");
			}
		} catch (IOException e) {
			throw new IOException("Failed to establish upload folder");
		}

		final Path uploadFolderPath = Paths.get(uploadFolder + File.separator + uploadfile.getName());
		logger.debug("File uploaded to {}", uploadFolderPath.toFile().getAbsolutePath());

		ExecutorService es = Executors.newFixedThreadPool(3);
		List<Future<Boolean>> futures = new ArrayList<>();
		final Map<String, String> properties = this.appProps;

		// ------- Adding file to Azure Storage -------
		logger.debug("Adding file to Azure Storage");
		Future<Boolean> azureFuture = es.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {

				Boolean upload = false;
				try {

					logger.debug("Starting Azure upload");
					AzureStorageUtil asu = new AzureStorageUtil(properties.get("StorageAccountName"), properties.get("StorageKey"));
					upload = asu.uploadFile(uploadFolderPath.toFile().getAbsolutePath(), uploadfile.getName());
					logger.debug("Upload to Azure complete: {}", upload);
				}
				catch(Exception e) {
					logger.error("ApiError in Azure upload: {}", e.getMessage(), e);
				}

				return upload;
			}
		});

		futures.add(azureFuture);

		boolean azureBool = false;

		try {

			logger.debug("Getting results for Azure and ADW uploads");
			azureBool = azureFuture.get();
		} catch (InterruptedException | ExecutionException e) {
			logger.error("ApiError in running executionservice: {}", e.getMessage(), e);
		}
		es.shutdown();

		try {
			if (!es.awaitTermination(1, TimeUnit.MINUTES)) {
				es.shutdownNow();
			}
		} catch (InterruptedException e) {

			Thread.currentThread().interrupt();
			logger.error("ApiError in shuttingdown executionservice: {}", e.getMessage(), e);
			es.shutdownNow();
		}
		return (azureBool ? true : false);
	}

	private static String saveUploadedFile(File file) throws IOException {

		Path tempDirPath = Files.createTempDirectory(StringUtils.EMPTY);

		String uploadFolder = tempDirPath.toString();

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line;
			StringBuffer sbtmp = new StringBuffer();
			while ((line = reader.readLine()) != null) {
				sbtmp.append(line);
			}

			byte[] bytes = sbtmp.toString().getBytes();
			Path path = Paths.get(uploadFolder + File.separator + file.getName());
			Files.write(path, bytes);
		}
		catch (IOException io) {
			throw io;
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (IOException io) {
					//ignore
				}
			}
		}
		return uploadFolder;
	}

	/**
	 * Validate user privileges by checking group membership. Must belong to group and a single Airline group.
	 * If the user doesn't belong to any group or belongs to more than one group, return null;
	 * otherwise, return airline name without "airline-" prefix
	 * @param authToken
	 * @return
	 */
	public String validateAndGetAirlineName(String authToken) {
		User currentUser = null;
		try {
			currentUser = this.getUserInfoFromJwtAccessToken(authToken);
		} catch (Exception ex) {}

		if (currentUser == null) {
			return null;
		}

		// Validate user privileges by checking group membership. Must belong to group and a single Airline group.
		List<Group> airlineGroups = currentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			return null;
		}

		String airline = airlineGroups.get(0).getDisplayName();
		logger.info("Airline Group: {}", airline);
		airline = airline.replace(Constants.AAD_GROUP_AIRLINE_PREFIX, "");
		return airline;
	}
}
