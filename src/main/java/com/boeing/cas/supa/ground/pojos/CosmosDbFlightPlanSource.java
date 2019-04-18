package com.boeing.cas.supa.ground.pojos;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.plist.XMLPropertyListConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class CosmosDbFlightPlanSource {

	private static final String SERVER_NAME = "ServerName";
	private static final String USER_NAME = "UserName";
	private static final String DATABASE_NAME = "DatabaseName";
	private static final String COLLECTION_NAME = "CollectionName";
	private static final String PRIMARY_PASSWORD = "PrimaryPassword";

	private String airline;
	private String serverName;
	private String userName;
	private String databaseName;
	private String collectionName;
	private String primaryPassword;

	private final Logger logger = LoggerFactory.getLogger(CosmosDbFlightPlanSource.class);

	public CosmosDbFlightPlanSource(String airline) {
		logger.debug("create CosmosDbFlightPlanSource for:" + airline);
		this.airline = airline;
	}

	public void addFlightPlanSource(String plistData) throws ConfigurationException {

		XMLPropertyListConfiguration plist = new XMLPropertyListConfiguration();
		plist.load(new ByteArrayInputStream(plistData.getBytes(StandardCharsets.UTF_8)));
		this.serverName = plist.getString(SERVER_NAME);
		this.userName = plist.getString(USER_NAME);
		this.databaseName = plist.getString(DATABASE_NAME);
		this.collectionName = plist.getString(COLLECTION_NAME);
		this.primaryPassword = plist.getString(PRIMARY_PASSWORD);
		logger.debug(this.toString());
	}

	public String getAirline() {
		return airline;
	}

	public void setAirline(String airline) {
		this.airline = airline;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public String getPrimaryPassword() {
		return primaryPassword;
	}

	public void setPrimaryPassword(String primaryPassword) {
		this.primaryPassword = primaryPassword;
	}

	@Override
	public String toString() {
		return (SERVER_NAME + ":" + this.serverName + "," + USER_NAME + ":" + this.userName + ","
			+ DATABASE_NAME + ":" + this.databaseName + "," + COLLECTION_NAME + ":" + this.collectionName);
	}
}
