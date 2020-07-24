package com.boeing.cas.supa.ground.services;

import com.boeing.cas.supa.ground.dao.AircraftInfoDao;
import com.boeing.cas.supa.ground.pojos.AircraftConfiguration;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import java.util.Date;

@Service
public class AircraftPropertyService {
	private static final Logger LOG = LoggerFactory.getLogger(AircraftPropertyService.class);
	private static final String AircraftPropertyFailed = "AIRCRAFT_PROPERTY_FAILED";
	private final Logger logger = LoggerFactory.getLogger(AircraftPropertyService.class);

	@Autowired
	private AircraftInfoDao aircraftInfoDao;

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

	public boolean isUpdated(String authToken, String tailNumber, String timeStamp){

		// Convert to UTC string?
		Date lastModified = getLastModified(authToken, tailNumber);

		if(!lastModified.equals(timeStamp)){
			return true;
		}
		return false;
	}
	
	@Transactional(value = TxType.REQUIRES_NEW)
	public Object getAircraftProperty(String authToken, String tailNumber) {
		try {
			String airlineName =  azureADClientService.validateAndGetAirlineName(authToken);
			if (Strings.isNullOrEmpty(airlineName)) {
				return new ApiError(AircraftPropertyFailed, "User membership is ambiguous - cannot define user's airline", RequestFailureReason.UNAUTHORIZED); 
			}
			
			AircraftConfiguration aircraftConfiguration = aircraftInfoDao.getAircarftPropertiesByAirlineAndTailNumber(airlineName, tailNumber);
			if (aircraftConfiguration == null) {
				return null;
			}
			
			return new Gson().toJson(aircraftConfiguration);
		} catch (Exception ex) {
			LOG.error("Failed to get aircraft property. Error: " + ex);
		}
		
		return new ApiError(AircraftPropertyFailed, "Failed to get aircraft property", RequestFailureReason.INTERNAL_SERVER_ERROR);
	}
};