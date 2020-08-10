package com.boeing.cas.supa.ground.services;

import com.boeing.cas.supa.ground.dao.AircraftInfoDao;
import com.boeing.cas.supa.ground.dao.TspDao;
import com.boeing.cas.supa.ground.exceptions.FileDownloadException;
import com.boeing.cas.supa.ground.pojos.AircraftConfigRes;
import com.boeing.cas.supa.ground.pojos.AircraftConfiguration;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class AircraftPropertyService {
	private final Logger logger = LoggerFactory.getLogger(AircraftPropertyService.class);
	private static final String AircraftPropertyFailed = "AIRCRAFT_PROPERTY_FAILED";

	@Autowired
	private AircraftInfoDao aircraftInfoDao;

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
	public Object getAircraftProperty(String authToken, String tailNumber) {

		logger.debug("trying to get property for : " + tailNumber);

		try {
			String airlineName =  azureADClientService.validateAndGetAirlineName(authToken);

			logger.debug("airlineName: " + airlineName);

			if (Strings.isNullOrEmpty(airlineName)) {
				return new ApiError(AircraftPropertyFailed, "User membership is ambiguous - cannot define user's airline", RequestFailureReason.UNAUTHORIZED); 
			}
			
			AircraftConfiguration aircraftConfiguration = aircraftInfoDao.getAircraftPropertiesByAirlineAndTailNumber(airlineName, tailNumber);
			if (aircraftConfiguration == null) {
				logger.debug("!! UH OH WE DIDN:T GET AN AIRCRAFT PROPERTY");
				return null;
			}
			
			return new Gson().toJson(aircraftConfiguration);
		} catch (Exception ex) {
			logger.error("Failed to get aircraft property. Error: " + ex);
		}
		
		return new ApiError(AircraftPropertyFailed, "Failed to get aircraft property", RequestFailureReason.INTERNAL_SERVER_ERROR);
	}

	public Object getAircraftConfig(String authToken, String tailNumber){

		logger.debug(" we reached getAircraftConfig() !!");

		try {
			String airlineName =  azureADClientService.validateAndGetAirlineName(authToken);
			if (Strings.isNullOrEmpty(airlineName)) {
//				throw new ApiError(AircraftPropertyFailed, "User membership is ambiguous - cannot define user's airline", RequestFailureReason.UNAUTHORIZED);
				throw new IllegalArgumentException("Bad Input");
			}
			/**
			 *  **NOTE: currently getTSP from Azure blob storage - future state get from SQL
			 */

			// actually aircraft property
			AircraftConfiguration aircraftConfiguration = aircraftInfoDao.getAircraftPropertiesByAirlineAndTailNumber(airlineName, tailNumber);
			if (aircraftConfiguration == null) {
				return null;
			}
			logger.debug("aircraftProperty: " + aircraftConfiguration);

			// get TSP from blob storage - it is json file tho?
			File tsp = getTSPFromBlob(airlineName, tailNumber);
			// I am trying to cast generic 'Object' of AircraftConfiguration into File
			File aircraftProperty = new File(new Gson().toJson(aircraftConfiguration));;

			// create zip file for TSP + AircraftProperty (json and text)
			List<File> srcFiles = Arrays.asList(tsp, aircraftProperty);

			FileOutputStream fos = new FileOutputStream("tsp.zip"); // naming convention for TSP?
			CheckedOutputStream checksum = new CheckedOutputStream(fos, new Adler32());
			ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(checksum));

			logger.debug("**** trying to Zip");
			for (File srcFile : srcFiles) {
				FileSystemResource resource = new FileSystemResource(srcFile);
				FileInputStream fis = new FileInputStream(srcFile);
				ZipEntry zipEntry = new ZipEntry(resource.getFilename());
				zipEntry.setSize(resource.contentLength());

				zipOut.putNextEntry(zipEntry);

				byte[] bytes = new byte[1024];
				int length;
				while((length = fis.read(bytes)) >= 0) {
					zipOut.write(bytes, 0, length);
				}
				fis.close();
				StreamUtils.copy(resource.getInputStream(), zipOut);
			}
			zipOut.close();
			fos.close();

			// pass zip file into checksum
			// String checkSum = CheckSumUtil.getSHA256(zipOut);
			String checkSumValue = String.valueOf(checksum.getChecksum().getValue());
			logger.debug("**CheckSum: " + checkSumValue);

			// response will have zip file + checksum in header, also return lastModified (between TSP + aircraft property)
			AircraftConfigRes res = new AircraftConfigRes(checkSumValue, aircraftProperty, zipOut);

			return res;

		} catch (Exception ex) {
			logger.error("Failed to get aircraft property. Error: " + ex);
		}

		return new ApiError(AircraftPropertyFailed, "Failed to get aircraft property", RequestFailureReason.INTERNAL_SERVER_ERROR);
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
