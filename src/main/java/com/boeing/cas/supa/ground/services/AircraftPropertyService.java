package com.boeing.cas.supa.ground.services;

import com.boeing.cas.supa.ground.dao.AircraftInfoDao;
import com.boeing.cas.supa.ground.dao.TspDao;
import com.boeing.cas.supa.ground.pojos.AircraftConfiguration;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import java.util.Date;
import java.util.List;

import static com.boeing.cas.supa.ground.services.FileManagementService.TSP_CONFIG_ZIP_CONTAINER;

@Service
public class AircraftPropertyService {
	private final Logger logger = LoggerFactory.getLogger(AircraftPropertyService.class);
	public static final String AircraftPropertyFailed = "AIRCRAFT_PROPERTY_FAILED";

	@Autowired
	private AircraftInfoDao aircraftInfoDao;

	@Autowired
	private TspManagementService tspManagementService;

	@Autowired
	private FileManagementService fileManagementService;

	@Autowired
	private TspDao tspDao;

	@Autowired
	private AzureADClientService aadClient;

	@Autowired
	private AzureADClientService azureADClientService;

	public Date getLastModified(String authToken){
		try{
			String airlineName =  azureADClientService.validateAndGetAirlineName(authToken);
			if (Strings.isNullOrEmpty(airlineName)) {
				logger.debug(":::UNAUTHORIZED - User membership is ambiguous - cannot define user's airline");
			}

			logger.debug("getLastModified airline name: " + airlineName);
			Date modifiedTime = aircraftInfoDao.getAircraftPropertyLastModifiedTimeStamp(airlineName);

			return modifiedTime;
		}catch(Exception ex){

		}
		return null;
	}

	public boolean isUpdated(String authToken, String tailNumber, Date timeStamp){

		// Convert to UTC string?
		Date lastModified = getLastModified(authToken);

		if(!lastModified.equals(timeStamp)){
			return true;
		}
		return false;
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public String getAircraftProperty(String authToken, String tailNumber) {

		try {
			String airlineName =  azureADClientService.validateAndGetAirlineName(authToken);

			AircraftConfiguration aircraftConfiguration = aircraftInfoDao.getAircraftPropertiesByAirlineAndTailNumber(airlineName, tailNumber);
			if (aircraftConfiguration == null) {
				logger.debug("!! UH OH WE DIDN:T GET AN AIRCRAFT PROPERTY");
				return null;
			}
			return new Gson().toJson(aircraftConfiguration);
		} catch (Exception ex) {
			logger.error("Failed to get aircraft property. Error: " + ex);
		}
		return null;
	}

	@Transactional
	public List<AircraftConfiguration> getAircraftPropertiesByAirline(String authToken){
		try {
			String airlineName = azureADClientService.validateAndGetAirlineName(authToken);

			List<AircraftConfiguration> aircraftConfigList = aircraftInfoDao.getAircraftPropertiesByAirline(airlineName);

			if(aircraftConfigList == null){
				logger.debug("Something wrong getting AP list");
				return null;
			}
			logger.debug("we got the list....");
			logger.debug(aircraftConfigList.toString());

			return aircraftConfigList;
		}catch(Exception ex){
			logger.error("failed to retrieve APs: " + ex);
		}
		return null;
	}

	public Date getLastModifiedTimeInContainer(String authToken, String airlineGroupTspContainer){
		try {
			String airlineName =  azureADClientService.validateAndGetAirlineName(authToken);
			if (Strings.isNullOrEmpty(airlineName)) {
				throw new IllegalArgumentException("Bad Input");
			}

			List<String> tspList = fileManagementService.getTspListFromStorage(authToken);

			logger.debug("tsplist");
			Date lastModifiedDate = null;

			for(String tspName : tspList) {
				Date thisModifiedDate = fileManagementService.getBlobLastModifiedTimeStamp(airlineGroupTspContainer, tspName);

				if(lastModifiedDate == null || thisModifiedDate.compareTo(lastModifiedDate) > 0){
					lastModifiedDate = thisModifiedDate;
				}
			}

			logger.debug("latest date: " + lastModifiedDate);

			return lastModifiedDate;
		} catch (Exception ex) {
			logger.error("Failed get container lastest date TSP package" + ex);
		}
		return null;
	}

	public byte[] getAircraftConfig(String authToken){
		try {
			String airlineName =  azureADClientService.validateAndGetAirlineName(authToken);
			if (Strings.isNullOrEmpty(airlineName)) {
				throw new IllegalArgumentException("Bad Input");
			}
			/**
			 *  **NOTE: currently getTSP from Azure blob storage - future state get from SQL
			 *  Zip Package contains all TSPs (.json) for an airline, in addition to all properties files
			 */
			 List<String> tspList = fileManagementService.getTspListFromStorage(authToken);
			 // JSON String?
//			 String aircraftPropertiesList = this.getAircraftPropertiesByAirline(authToken);
			 byte[] zipFile = fileManagementService.zipFileList(tspList, authToken);

			 logger.debug(" GOOD - zip successful in aircraft property service....");

			 return zipFile;
		} catch (Exception ex) {
			logger.error("Failed create TSP package" + ex);
		}
		return null;
	}

	public byte[] getAircraftConfigFromBlob(String authToken, String fileName){
		try {
			String airlineName =  azureADClientService.validateAndGetAirlineName(authToken);
			if (Strings.isNullOrEmpty(airlineName)) {
				throw new IllegalArgumentException("Bad Input");
			}
			/**
			 *  **NOTE: currently getTSP from Azure blob storage - future state get from SQL
			 *  Zip Package contains all TSPs (.json) for an airline, in addition to all properties files
			 */
//			List<String> tspList = fileManagementService.getTspListFromStorage(authToken);
//			byte[] zipFile = fileManagementService.zipFileList(tspList, authToken);

			byte[] tspConfig = fileManagementService.getFileFromStorage(fileName, TSP_CONFIG_ZIP_CONTAINER, authToken);

			logger.debug(" GOOD - Zip package Downloaded");

			return tspConfig;
		} catch (Exception ex) {
			logger.error("Failed to Download Zip Package" + ex);
		}
		return null;
	}
};
