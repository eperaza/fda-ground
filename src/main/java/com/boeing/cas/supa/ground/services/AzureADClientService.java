package com.boeing.cas.supa.ground.services;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.mail.internet.MimeMessage;
import javax.net.ssl.HttpsURLConnection;

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
import org.springframework.web.bind.annotation.RequestMethod;

import com.boeing.cas.supa.ground.dao.UserAccountRegistrationDao;
import com.boeing.cas.supa.ground.exceptions.ElevatedPermissionException;
import com.boeing.cas.supa.ground.exceptions.MobileConfigurationException;
import com.boeing.cas.supa.ground.exceptions.UserAccountRegistrationException;
import com.boeing.cas.supa.ground.helpers.AzureADClientHelper;
import com.boeing.cas.supa.ground.helpers.HttpClientHelper;
import com.boeing.cas.supa.ground.pojos.Error;
import com.boeing.cas.supa.ground.pojos.Group;
import com.boeing.cas.supa.ground.pojos.NewUser;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.pojos.UserAccountActivation;
import com.boeing.cas.supa.ground.pojos.UserAccountRegistration;
import com.boeing.cas.supa.ground.pojos.UserRegistration;
import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.boeing.cas.supa.ground.utils.Constants;
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

	public Object getAccessTokenFromUserCredentials(String username, String password) {

		AuthenticationResult result = null;

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
			}
		} catch (AuthenticationException ae) {
			logger.error("AuthenticationException: {}", ae.getMessage(), ae);
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
				return new Error("PERMISSIONS_FAILURE", e.getMessage());
			}
		}
		else if (permissionType == PermissionType.IMPERSONATION) {

			// Get access token based on delegated permission via impersonation with a Local Administrator of the tenant.
			Object authResult = getAccessTokenFromUserCredentials(this.appProps.get("UserManagementAdminUsername"), this.appProps.get("UserManagementAdminPassword"));
			if (authResult instanceof AuthenticationResult) {
				accessToken = ((AuthenticationResult) authResult).getAccessToken();
			} else {
				logger.error("Failed to obtain access tokens via impersonation");
				return new Error("PERMISSIONS_FAILURE", "Failed to impersonate admin user");
			}
		}
		else {
			return new Error("PERMISSIONS_FAILURE", "Missing or invalid permissions context");
		}

		return accessToken;
	}

	public String getGroupIdByName(String groupName, String applicationToken) {

		String groupId = null;

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
				JsonNode valuesNode = mapper.readTree(responseStr).path("value");
				if (valuesNode.isArray()) {
					groupId = valuesNode.get(0).path("objectId").asText();
				}
			}
		}
		catch (Exception e) {
			logger.error("Failed to query groups: {}", e.getMessage(), e);
		}
		
		return groupId;
	}

	public Object createUser(NewUser newUserPayload, String accessTokenInRequest, Group airlineGroup, String roleGroupName) {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Create user -");

		// Get GroupId of requested user role
		String roleGroupId = getGroupIdByName(newUserPayload.getRoleGroupName(), accessTokenInRequest);
		if (roleGroupId == null) {
			return new Error("CREATE_USER_FAILURE", "Missing or invalid user role");
		}

		// Get access token based on delegated permission via impersonation with a Local Administrator of the tenant.
		String accessToken = null;
		Object authResult = getElevatedPermissionsAccessToken(PermissionType.IMPERSONATION);
		if (authResult instanceof Error) {
			return authResult;
		}
		// access token could not be obtained either via delegated permissions or application permissions.
		else if (authResult == null) {
			return new Error("CREATE_USER_FAILURE", "Failed to acquire permissions");
		}
		else {
			accessToken = String.valueOf(authResult);
			progressLog.append("\nObtained impersonation access token");
		}

		// Proceed with request 
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
			conn.setRequestProperty("Authorization", accessToken);
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
				boolean addedUserToAirlineGroup = addUserToGroup(airlineGroup.getObjectId(), newlyCreatedUser.getObjectId(), accessToken);
				progressLog.append("\nAdded user to airline group");
				boolean addedUserToRoleGroup = addUserToGroup(roleGroupId, newlyCreatedUser.getObjectId(), accessToken);
				progressLog.append("\nAdded user to primary user role");

				// Get the updated user membership and update the User object sent back in the response.
				List<Group> groups = getUserGroupMembershipFromGraph(newlyCreatedUser.getObjectId(), accessToken);
				progressLog.append("\nObtained groups which user is a member of");
				newlyCreatedUser.setGroups(groups);

				if (!addedUserToAirlineGroup || !addedUserToRoleGroup) {
					logger.error("Failed to add user to airline group and/or role group");
				}
				progressLog.append("\nUser added to airline and role groups");

				// User creation looks good... set new user to the return value [resultObj]
				resultObj = newlyCreatedUser;
				logger.info("Added new user {} to {} and {} groups", newlyCreatedUser.getUserPrincipalName(), airlineGroup.getDisplayName(), roleGroupName);

				// Register new user in the account registration database
				String registrationToken = UUID.randomUUID().toString();
				UserAccountRegistration registration = new UserAccountRegistration(registrationToken, newlyCreatedUser.getObjectId(), newlyCreatedUser.getUserPrincipalName(), airlineGroup.getDisplayName(), newUserPayload.getOtherMails().get(0), Constants.UserAccountState.PENDING_USER_ACTIVATION.toString());
				userAccountRegister.registerNewUserAccount(registration);
				progressLog.append("\nRegistered new user account in database");
				logger.info("Registered new user {} in the account registration database", newlyCreatedUser.getUserPrincipalName());
				
				// New user account registration is successful. Now send email to user (use otherMail address)
				MimeMessage message = emailSender.createMimeMessage();
				MimeMessageHelper helper = new MimeMessageHelper(message);
				helper.setFrom(new StringBuilder("support@").append(this.appProps.get("AzureADTenantName")).toString());
				helper.setReplyTo(new StringBuilder("support@").append(this.appProps.get("AzureADTenantName")).toString());
				helper.setSubject("FD Advisor user account activation");
				helper.setTo(newUserPayload.getOtherMails().get(0));
				helper.setText(composeNewUserAccountActivationEmail(newlyCreatedUser, registrationToken), true);
				emailSender.send(message);
				logger.info("Sent account activation email to new user {}", newlyCreatedUser.getUserPrincipalName());
				progressLog.append("\nSuccessfully queued email for delivery");
			}
			else {

				JsonNode errorNode = mapper.readTree(responseStr).path("odata.error");
				JsonNode messageNode = errorNode.path("message");
				JsonNode valueNode = messageNode.path("value");

				resultObj = new Error("USER_CREATE_FAILED", valueNode.asText());
			}
		} catch (UserAccountRegistrationException uare) {
			logger.error("Failed to register new user account: {}", uare.getMessage(), uare);
			resultObj = new Error("USER_CREATE_FAILED", "FDAGNDSVCERR0001");
		} catch (JsonProcessingException jpe) {
			logger.error("JsonProcessingException: {}", jpe.getMessage(), jpe);
			resultObj = new Error("USER_CREATE_FAILED", "FDAGNDSVCERR0002");
		} catch (MalformedURLException murle) {
			logger.error("MalformedURLException: {}", murle.getMessage(), murle);
			resultObj = new Error("USER_CREATE_FAILED", "FDAGNDSVCERR0004");
		} catch (ProtocolException pe) {
			logger.error("ProtocolException: {}", pe.getMessage(), pe);
			resultObj = new Error("USER_CREATE_FAILED", "FDAGNDSVCERR0008");
		} catch (IOException ioe) {
			logger.error("IOException: {}", ioe.getMessage(), ioe);
			resultObj = new Error("USER_CREATE_FAILED", "FDAGNDSVCERR0016");
		} catch (MailException me) {
			logger.error("Failed to send email: {}", me.getMessage(), me);
			resultObj = new Error("USER_CREATE_FAILED", "FDAGNDSVCERR0032");
		} catch (Exception e) {

			Throwable nestedException = null;
			if ((nestedException = e.getCause()) != null) {
				logger.error("Failed to complete user creation flow: {}", nestedException.getMessage(), nestedException);
			}
			else {
				logger.error("Failed to complete user creation flow: {}", e.getMessage(), e);
			}
			resultObj = new Error("USER_CREATE_FAILED", "FDAGNDSVCERR0064");
		} finally {

			if (resultObj instanceof Error) {
				logger.error("FDAGndSvcLog> {}", progressLog.toString());
			}
		}

		return resultObj;
	}

	public Object getUsers(String accessTokenInRequest) {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Delete user -");

		try {

			// Get group membership of user issuing request. Ensure that user belongs to role-airlinefocal group
			 // and one and only one airline group.
			User airlineFocalCurrentUser = getUserInfoFromJwtAccessToken(accessTokenInRequest);

			// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
			List<Group> airlineGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith("airline-")).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());
			List<Group> roleAirlineFocalGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().equals("role-airlinefocal")).peek(g -> logger.info("Role Group: {}", g)).collect(Collectors.toList());
			if (airlineGroups.size() != 1 || roleAirlineFocalGroups.size() != 1) {
				return new Error("USERS_LIST_FAILED", "User membership is ambiguous", RequestFailureReason.UNAUTHORIZED);
			}
			progressLog.append("\nRequesting user belongs to single airline group and airline focal role group");

			// Get access token based on delegated permission via impersonation with a Local Administrator of the tenant.
			String accessToken = null;
			Object authResult = getElevatedPermissionsAccessToken(PermissionType.IMPERSONATION);
			if (authResult instanceof Error) {
				return authResult;
			}
			// access token could not be obtained either via delegated permissions or application permissions.
			else if (authResult == null) {
				return new Error("USERS_LIST_FAILED", "Failed to acquire permissions", RequestFailureReason.UNAUTHORIZED);
			}
			else {
				accessToken = String.valueOf(authResult);
				progressLog.append("\nObtained impersonation access token");
			}

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
			if (connListUsers.getResponseCode() == HttpStatus.OK.value() && (errorNode == null || errorNode instanceof MissingNode)) {

				List<User> membersOfAirlineGroup = new ArrayList<>();
				logger.info("List of users in {} group in Azure AD", airlineGroups.get(0).getDisplayName());
				Iterator<JsonNode> iterator = mapper.readTree(responseStr).path("value").iterator();
				while (iterator.hasNext()) {
					JsonNode nextElemNode = iterator.next();
					if (nextElemNode.path("objectType").asText().equals("User")) {

						User member = mapper.readValue(nextElemNode.toString(), User.class);
						if (member.getObjectId().equals(airlineFocalCurrentUser.getObjectId())) {
							continue;
						}
						membersOfAirlineGroup.add(member);
					}
				}
				resultObj = mapper.setSerializationInclusion(Include.NON_NULL).setSerializationInclusion(Include.NON_EMPTY)
						.writeValueAsString(membersOfAirlineGroup);
				progressLog.append("\nObtained members of group in Azure AD");
			}
		} catch (JsonProcessingException jpe) {
			logger.error("JsonProcessingException: {}", jpe.getMessage(), jpe);
			resultObj = new Error("USERS_LIST_FAILED", "FDAGNDSVCERR0002");
		} catch (MalformedURLException murle) {
			logger.error("MalformedURLException: {}", murle.getMessage(), murle);
			resultObj = new Error("USERS_LIST_FAILED", "FDAGNDSVCERR0004");
		} catch (ProtocolException pe) {
			logger.error("ProtocolException: {}", pe.getMessage(), pe);
			resultObj = new Error("USERS_LIST_FAILED", "FDAGNDSVCERR0008");
		} catch (IOException ioe) {
			logger.error("IOException: {}", ioe.getMessage(), ioe);
			resultObj = new Error("USERS_LIST_FAILED", "FDAGNDSVCERR0016");
		} finally {
			
			if (resultObj instanceof Error) {
				logger.error("FDAGndSvcLog> {}", progressLog.toString());
			}
		}
		
		return resultObj;
	}
	
	public Object deleteUser(String userId, String accessTokenInRequest) {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Delete user -");

		try {

			// Get group membership of user issuing request. Ensure that user belongs to role-airlinefocal group
			 // and one and only one airline group.
			User airlineFocalCurrentUser = getUserInfoFromJwtAccessToken(accessTokenInRequest);
			
			// Ensure requesting user is not trying to delete self!
			if (airlineFocalCurrentUser.getObjectId().equals(userId)) {
				return new Error("USER_DELETE_FAILED", "Airline focal user cannot delete self", RequestFailureReason.BAD_REQUEST);
			}

			// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
			List<Group> airlineGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith("airline-")).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());
			List<Group> roleAirlineFocalGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().equals("role-airlinefocal")).peek(g -> logger.info("Role Group: {}", g)).collect(Collectors.toList());
			if (airlineGroups.size() != 1 || roleAirlineFocalGroups.size() != 1) {
				return new Error("USER_DELETE_FAILED", "User membership is ambiguous", RequestFailureReason.UNAUTHORIZED);
			}
			progressLog.append("\nRequesting user belongs to single airline group and airline focal role group");

			// Get access token based on delegated permission via impersonation with a Local Administrator of the tenant.
			String accessToken = null;
			Object authResult = getElevatedPermissionsAccessToken(PermissionType.IMPERSONATION);
			if (authResult instanceof Error) {
				return authResult;
			}
			// access token could not be obtained either via delegated permissions or application permissions.
			else if (authResult == null) {
				return new Error("USER_DELETE_FAILED", "Failed to acquire permissions", RequestFailureReason.UNAUTHORIZED);
			}
			else {
				accessToken = String.valueOf(authResult);
				progressLog.append("\nObtained impersonation access token");
			}

			// Ensure another airline focal user is not being deleted, by getting group membership
			User deleteUser = getUserInfoFromGraph(userId, accessToken);
			if (deleteUser == null) {
				return new Error("USER_DELETE_FAILED", "Not allowed to delete user if not in same airline group", RequestFailureReason.NOT_FOUND);
			}
			List<Group> deleteUserGroups = getUserGroupMembershipFromGraph(deleteUser.getObjectId(), accessToken);
			if (deleteUserGroups != null) {

				List<Group> deleteUserAirlineGroups = deleteUserGroups.stream().filter(g -> g.getDisplayName().toLowerCase().startsWith("airline-")).collect(Collectors.toList());
				List<Group> deleteUserRoleGroups = deleteUserGroups.stream().filter(g -> g.getDisplayName().toLowerCase().startsWith("role-")).collect(Collectors.toList());
				if (deleteUserAirlineGroups.size() != 1 || !deleteUserAirlineGroups.get(0).getObjectId().equals(airlineGroups.get(0).getObjectId())) {
					return new Error("USER_DELETE_FAILED", "Not allowed to delete user if not in same airline group", RequestFailureReason.UNAUTHORIZED);
				}
				else if (deleteUserRoleGroups.stream().filter(g -> g.getDisplayName().toLowerCase().equals("role-airlinefocal")).collect(Collectors.toList()).size() > 0) {
					return new Error("USER_DELETE_FAILED", "Not allowed to delete another airline focal user", RequestFailureReason.UNAUTHORIZED);
				}
				else {
					// continue
				}
			}
			else {
				return new Error("USER_DELETE_FAILED", "Not allowed to delete user if not in same airline group", RequestFailureReason.UNAUTHORIZED);
			}
			progressLog.append("\nUser to be deleted is in same airline group and is not another airline focal");

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
			connDeleteUser.setRequestProperty("Authorization", accessToken);
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
			resultObj = new Error("USER_DELETE_FAILED", "FDAGNDSVCERR0001");
		} catch (JsonProcessingException jpe) {
			logger.error("JsonProcessingException: {}", jpe.getMessage(), jpe);
			resultObj = new Error("USER_DELETE_FAILED", "FDAGNDSVCERR0002");
		} catch (MalformedURLException murle) {
			logger.error("MalformedURLException: {}", murle.getMessage(), murle);
			resultObj = new Error("USER_DELETE_FAILED", "FDAGNDSVCERR0004");
		} catch (ProtocolException pe) {
			logger.error("ProtocolException: {}", pe.getMessage(), pe);
			resultObj = new Error("USER_DELETE_FAILED", "FDAGNDSVCERR0008");
		} catch (IOException ioe) {
			logger.error("IOException: {}", ioe.getMessage(), ioe);
			resultObj = new Error("USER_DELETE_FAILED", "FDAGNDSVCERR0016");
		} finally {
			
			if (resultObj instanceof Error) {
				logger.error("FDAGndSvcLog> {}", progressLog.toString());
			}
		}

		return resultObj;
	}

	public Object enableUserAndSetPassword(UserAccountActivation userAccountActivation) {

		Object resultObj = null;
		StringBuilder progressLog = new StringBuilder("Activate user -");

		HttpsURLConnection connGetUser = null, connEnableUser = null;
		try {

			// Check if token exists and corresponds to PENDING_USER_ACTIVATION in the database
			boolean isUserNotActivated = userAccountRegister.isUserAccountNotActivated(userAccountActivation.getRegistrationToken(), userAccountActivation.getUsername(), Constants.UserAccountState.PENDING_USER_ACTIVATION.toString());
			if (!isUserNotActivated) {
				logger.warn("User registration record/token not found");
				throw new UserAccountRegistrationException("Invalid or expired user account activation token");
			}
			progressLog.append("\nUser account registration record/token found");

			// Check if user corresponding to userPrincipalName exists in Azure AD and is currently not enabled.
			// Get access token based on delegated permission via impersonation with a Local Administrator of the tenant.
			String accessToken = null;
			Object elevatedAuthResult = getElevatedPermissionsAccessToken(PermissionType.IMPERSONATION);
			if (elevatedAuthResult instanceof Error) {
				throw new ElevatedPermissionException(((Error) elevatedAuthResult).getErrorDescription());
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
					throw new UserAccountRegistrationException("User account is already activated");
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
				throw new UserAccountRegistrationException("Failed to enable user and set password");
			}

			// Update UserAccountRegistrations database table to invalidate token (by updating account state)
			userAccountRegister.enableNewUserAccount(userAccountActivation.getRegistrationToken(), userObj.getUserPrincipalName(), Constants.UserAccountState.PENDING_USER_ACTIVATION.toString(), Constants.UserAccountState.USER_ACTIVATED.toString());
			progressLog.append("\nActivated new user account in database");

			Object ar = getAccessTokenFromUserCredentials(userObj.getUserPrincipalName(), userAccountActivation.getPassword());
			if (ar == null) {
				throw new UserAccountRegistrationException("Failed to obtain OAuth2 tokens with user credentials");
			}
			
			if (ar instanceof AuthenticationResult) {

				AuthenticationResult authResult = (AuthenticationResult) ar;
				String getPfxEncodedAsBase64 = this.appProps.get("client2base64");
				String getPlistFromBlob = getPlistFromBlob("preferences", "ADW.plist");
				String mobileConfigFromBlob = getPlistFromBlob("config", "supaConfigEFO.mobileconfig");
				if (getPlistFromBlob != null && mobileConfigFromBlob != null) {
					UserRegistration userReg = new UserRegistration(authResult, getPfxEncodedAsBase64, getPlistFromBlob, mobileConfigFromBlob);
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
		} catch (UserAccountRegistrationException uare) {
			logger.error("Failed to activate user account: {}", uare.getMessage());
			resultObj = new Error("ACTIVATE_USER_ACCOUNT_FAILURE", "FDAGNDSVCERR0001");
		} catch (AuthenticationException ae) {
			logger.error("Failed to activate user account: {}", ae.getMessage());
			resultObj = new Error("ACTIVATE_USER_ACCOUNT_FAILURE", "FDAGNDSVCERR0002");
		} catch (MobileConfigurationException mce) {
			logger.error("Failed to activate user account: {}", mce.getMessage());
			resultObj = new Error("ACTIVATE_USER_ACCOUNT_FAILURE", "FDAGNDSVCERR0001");
		} catch (ElevatedPermissionException epe) {
			logger.error("Failed to activate user account: {}", epe.getMessage());
			resultObj = new Error("ACTIVATE_USER_ACCOUNT_FAILURE", "FDAGNDSVCERR0004");
		} catch (MalformedURLException murle) {
			logger.error("MalformedURLException: {}", murle.getMessage(), murle);
			resultObj = new Error("ACTIVATE_USER_ACCOUNT_FAILURE", "FDAGNDSVCERR0008");
		} catch (ProtocolException pe) {
			logger.error("ProtocolException: {}", pe.getMessage(), pe);
			resultObj = new Error("ACTIVATE_USER_ACCOUNT_FAILURE", "FDAGNDSVCERR0016");
		} catch (IOException ioe) {
			logger.error("IOException: {}", ioe.getMessage(), ioe);
			resultObj = new Error("ACTIVATE_USER_ACCOUNT_FAILURE", "FDAGNDSVCERR0032");
		}

		return resultObj;
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

		logger.debug("-------> Access token for logged in user: {}", accessToken);
		
		String uniqueId = AzureADClientHelper.getUniqueIdFromJWT(accessToken);
		User user = getUserInfoFromGraph(uniqueId, accessToken);
		List<Group> group = getUserGroupMembershipFromGraph(uniqueId, accessToken);
		user.setGroups(group);
		
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

			logger.debug("-------> User response from graph API: {}", responseStr);
			
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

	public List<Group> getUserGroupMembershipFromGraph(String uniqueId, String accessToken) {

		logger.debug("Getting group object list info from graph");
		List<Group> groupList = new ArrayList<>();
		try {

			URL url = new URL(new StringBuilder(azureadApiUri).append('/').append(this.appProps.get("AzureADTenantName"))
					.append("/users/").append(uniqueId).append("/memberOf")
					.append('?').append(new StringBuilder(Constants.AZURE_API_VERSION_PREFIX)
							.append(azureadApiVersion).toString())
					.toString());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			// Set the appropriate header fields in the request header.
			conn.setRequestProperty("api-version", azureadApiVersion);
			conn.setRequestProperty("Authorization", accessToken);
			conn.setRequestProperty("Accept", Constants.ACCEPT_CT_JSON_ODATAMINIMAL);
			String responseStr = HttpClientHelper.getResponseStringFromConn(conn, true);
			int responseCode = conn.getResponseCode();
			if (responseCode == 200) {
				groupList = extractUserGroupMembershipFromResponse(responseStr);
			}
		} catch (MalformedURLException e) {
			logger.error("MalformedURLException while retrieving groups from graph: {}", e.getMessage(), e);
		} catch (IOException e) {
			logger.error("I/O Exception while retrieving groups from graph: {}", e.getMessage(), e);
		}

		return groupList;
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
					Group group = mapper.treeToValue(objNode, Group.class);
					groupList.add(group);
				}
			}
		} catch (IOException e) {
			logger.error("I/O Exception while reading groups from list: {}", e.getMessage(), e);
		}

		return groupList;
	}

	public ObjectNode convertNewUserObjectToJsonPayload(ObjectMapper mapper, NewUser newUserPayload) {
		
		ObjectNode rootNode = mapper.createObjectNode();
		if (newUserPayload.getUserPrincipalName().indexOf('@') < 0) {
			rootNode.put("userPrincipalName", new StringBuilder(newUserPayload.getUserPrincipalName()).append('@').append(this.appProps.get("AzureADTenantName")).toString());
			rootNode.put("mailNickname", newUserPayload.getUserPrincipalName());
		}
		else {
			rootNode.put("userPrincipalName", newUserPayload.getUserPrincipalName());
			rootNode.put("mailNickname", newUserPayload.getUserPrincipalName().substring(0, newUserPayload.getUserPrincipalName().indexOf('@')));
		}
		//rootNode.put("accountEnabled", newUserPayload.isAccountEnabled());
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
	
	private String composeNewUserAccountActivationEmail(User newlyCreatedUser, String registrationToken) {

		String base64EncodedPayload = Base64.getEncoder().encodeToString(new StringBuilder(newlyCreatedUser.getUserPrincipalName()).append(' ').append(registrationToken).toString().getBytes());
		StringBuilder emailMessageBody = new StringBuilder();
		emailMessageBody.append("Dear ").append(newlyCreatedUser.getDisplayName()).append(',');
		emailMessageBody.append(Constants.HTML_LINE_BREAK).append(Constants.HTML_LINE_BREAK);
		emailMessageBody.append("Please click on this link from your mobile device to complete your FDA user account activation:");
		emailMessageBody.append(Constants.HTML_LINE_BREAK).append(Constants.HTML_LINE_BREAK);
		emailMessageBody.append("<a href=\"iavfdaapplication://register?").append(base64EncodedPayload).append("\">").append("FD Advisor Account Activation").append("</a>");
		emailMessageBody.append(Constants.HTML_LINE_BREAK).append(Constants.HTML_LINE_BREAK);
		emailMessageBody.append("Thank you").append(Constants.HTML_LINE_BREAK).append("Flight Deck Advisor Support");
		
		return emailMessageBody.toString();
	}

    private String getPlistFromBlob(String containerName, String fileName) {

        String base64 = null;
        try {
            AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"), this.appProps.get("StorageKey"));
            try (ByteArrayOutputStream outputStream = asu.downloadFile(containerName, fileName)) {
                base64 = Base64.getEncoder().encodeToString(outputStream.toString().getBytes());
            }
        }
        catch (IOException ioe) {
            logger.error("Failed to retrieve Plist [{}]: {}", fileName, ioe.getMessage(), ioe);
        }

        return base64;
    }

	public Error getLoginErrorFromString(String responseString) {

		Error errorObj = null;

		try {

			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> map = mapper.readValue(responseString, new TypeReference<HashMap<String, String>>() {
			});
			logger.error("error response: {}", responseString);
			String errorDesc = map.get("error_description");
			String errorString = errorDesc.split("\\r?\\n")[0];
			errorObj = new Error(map.get("error"), errorString);
		} catch (IOException ioe) {
			logger.error("Failed to extract login error from string: {}", ioe.getMessage(), ioe);
			errorObj = new Error("Authentication failed", ioe.getMessage());
		}

		return errorObj;
	}
}
