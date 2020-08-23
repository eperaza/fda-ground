package com.boeing.cas.supa.ground.services;

import com.boeing.cas.supa.ground.dao.AircraftInfoDao;
import com.boeing.cas.supa.ground.dao.TspDao;
import com.boeing.cas.supa.ground.exceptions.FileDownloadException;
import com.boeing.cas.supa.ground.pojos.AircraftConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
	private AzureADClientService azureADClientService;

	public Date getLastModified(String authToken, String tailNumber){
		try{
			String airlineName =  azureADClientService.validateAndGetAirlineName(authToken);
			if (Strings.isNullOrEmpty(airlineName)) {
				logger.debug(":::UNAUTHORIZED - User membership is ambiguous - cannot define user's airline");
			}

			// Convert to UTC string?
			Date modifiedTime = aircraftInfoDao.getAircraftPropertyLastModifiedTimeStamp(airlineName, tailNumber);

			return modifiedTime;

		}catch(Exception ex){

		}
		return null;
	}

	public boolean isUpdated(String authToken, String tailNumber, Date timeStamp){

		// Convert to UTC string?
		Date lastModified = getLastModified(authToken, tailNumber);

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
	public List<String> getAircraftPropertiesByAirline(String authToken){
		try {
			String airlineName = azureADClientService.validateAndGetAirlineName(authToken);

			logger.debug("got to new method with airlineName: " + airlineName);

			Set<AircraftConfiguration> aircraftConfigList = (Set<AircraftConfiguration>) aircraftInfoDao.getAircraftPropertiesByAirline(airlineName);
			if(aircraftConfigList == null){
				logger.debug("Something wrong getting AP list");
				return null;
			}
			List<String> results = new ArrayList<>();
			for(AircraftConfiguration apConfig: aircraftConfigList){
				results.add(new Gson().toJson(apConfig));
			}
			return results;
		}catch(Exception ex){
			logger.error("failed to retrieve APs: " + ex);
		}
		return null;
	}

	public byte[] getAircraftConfig(String authToken){
		try {

			logger.debug("DO WE EVEN GET HERE 2");

			String airlineName =  azureADClientService.validateAndGetAirlineName(authToken);
			if (Strings.isNullOrEmpty(airlineName)) {
				throw new IllegalArgumentException("Bad Input");
			}
			/**
			 *  **NOTE: currently getTSP from Azure blob storage - future state get from SQL
			 *  Zip Package contains all TSPs (.json) for an airline, in addition to all properties files
			 */
			 List<String> tspList = fileManagementService.getTspListFromStorage(authToken);
			 byte[] zipFile = fileManagementService.zipFileList(tspList, authToken);

			 logger.debug(" GOOD - zip successful in aircraft property service....");

			 return zipFile;
		} catch (Exception ex) {
			logger.error("Failed create TSP package" + ex);
		}
		return null;
	}

	public File getTSPFromBlob(String airlineName, String tailNumber) throws FileDownloadException {
		// get TSP from blob storage - it is json file tho?
		logger.debug("Grabbing TSP from Blob");

		String fileName = tailNumber + ".json";
		File tsp = azureADClientService.getFileFromBlob("tsp", fileName, airlineName);

		ObjectMapper tspObj = new ObjectMapper();
		try {
			// convert TSP to json
			String tspJson = tspObj.writeValueAsString(tsp);

			logger.debug("********** tsp JSON ======");
			logger.debug(tspJson);

		} catch (IOException e) {
			e.printStackTrace();
			logger.debug("exception caught converting TSP to json: " + e);
		}
		logger.debug("tsp found: " + tsp);
		return tsp;
	}

	public static void writeToZipFile(String path, ZipOutputStream zipStream) throws FileNotFoundException, IOException {
		System.out.println("Writing file : '" + path + "' to zip file");

		File aFile = new File(path);
		FileInputStream fis = new FileInputStream(aFile);
		ZipEntry zipEntry = new ZipEntry(path);
		zipStream.putNextEntry(zipEntry);
		byte[] bytes = new byte[1024];
		int length;

		while ((length = fis.read(bytes)) >= 0) {
			zipStream.write(bytes, 0, length);
		}
		zipStream.closeEntry();
		fis.close();
	}

};
