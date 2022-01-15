package com.boeing.cas.supa.ground.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import com.boeing.cas.supa.ground.dao.FeatureManagementDao;
import com.boeing.cas.supa.ground.exceptions.FeatureManagementException;
import com.boeing.cas.supa.ground.exceptions.FileDownloadException;
import com.boeing.cas.supa.ground.exceptions.TspConfigLogException;
import com.boeing.cas.supa.ground.pojos.AirlinePreferences;
import com.boeing.cas.supa.ground.pojos.AirlineStatusChecklistItem;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.CosmosDbFlightPlanSource;
import com.boeing.cas.supa.ground.pojos.Group;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static com.boeing.cas.supa.ground.services.FileManagementService.TSP_CONFIG_ZIP_CONTAINER;

@Service
public class AirlineStatusService {
    
    @Autowired
    private AzureADClientService azureADClientService;

    @Autowired
    private Map<String, String> appProps;

    @Autowired
    FileManagementService fileManagementService;

    @Autowired
	private FeatureManagementDao featureManagementDao;

    private final Logger logger = LoggerFactory.getLogger(AirlineStatusService.class);
    private static final String FLIGHT_PLAN_CONTAINER = "flight-plan-source";
    private static final String AIRLINE_STATUS_SUCCESS = "[OK]";
    private static final String AIRLINE_STATUS_ERROR = "[ERROR]";
    private static final String AIRLINE_STATUS_UNAUTHORIZED = "[UNAUTHORIZED]";
    private static final String USER_GROUP_AUTHORIZED = "fda";
    private static final String NON_FDA_MEMBER ="User not authorized, belongs to: {}";

    /**
	 * checkAutoConfig - gets the airline TSP package status
	 * @param authToken access token
     * @param airline airline code
	 * @return airline TSP status 
     * @throws FileDownloadException 
	 */
    @Async("asyncExecutor")
    public CompletableFuture<Map<List<AirlineStatusChecklistItem>, HttpStatus>> checkAutoConfig(String authToken, String airline) throws IOException, NoSuchAlgorithmException, TspConfigLogException, FileDownloadException, InterruptedException{
        logger.info("Auto Config package check starting.."); 
        AirlineStatusChecklistItem checklistItem = new AirlineStatusChecklistItem();
        List<AirlineStatusChecklistItem> list = new ArrayList<>();
        Map<List<AirlineStatusChecklistItem>, HttpStatus> response = new LinkedHashMap<>();
        Date lastModified = null;
        
        // Validate user
        final User user = azureADClientService.getUserInfoFromJwtAccessToken(authToken);

        List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
        if (airlineGroups.size() != 1) {
            throw new FileDownloadException(new ApiError("FILE_DOWNLOAD_FAILURE", "Failed to associate user with an airline", Constants.RequestFailureReason.UNAUTHORIZED));
        }
        
        String userGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);
        logger.info("User belongs to: {}", userGroup);

        if (userGroup.equals(USER_GROUP_AUTHORIZED)){
            String airlineGroup = airline.toLowerCase();
            try {
                AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"), this.appProps.get("StorageKey"));

                String container = TSP_CONFIG_ZIP_CONTAINER;

                // File path name to retrieve from blob - it is not truly a real directory
                String fileName = new StringBuilder(airlineGroup).append("/").append(airlineGroup).append("-config-pkg.zip").toString();
                boolean tspExists = asu.blobExistsOnCloud(container, fileName);

                logger.info("TSP File Name: {}", fileName);
                logger.warn("TSP Exists: {}", ((tspExists == true) ? "Yes" : "No" ));
                
                checklistItem.setItem("TSP package");

                // If tsp exists get last modified
                if (tspExists) {
                    lastModified = fileManagementService.getBlobLastModifiedTimeStamp(container, fileName);
                    logger.debug("retrieved timestamp: " + lastModified.toString());
                    
                    List<Object> content = new ArrayList<>();
                    Map<String, String> description = new HashMap<>();
                    description.put("lastModified", lastModified.toString());

                    final ObjectMapper mapper = new ObjectMapper(); 
                    final Object obj = mapper.convertValue(description, Object.class);

                    content.add(obj);
                    checklistItem.setContent(content);
                    checklistItem.setStatus(AIRLINE_STATUS_SUCCESS);
                    list.add(checklistItem);

                    response.put(list, HttpStatus.OK);
                    logger.info("TSP package setup is good");

                } 
                else {
                    checklistItem.setStatus(AIRLINE_STATUS_ERROR);
                    list.add(checklistItem);
                    response.put(list, HttpStatus.INTERNAL_SERVER_ERROR);
                    logger.error("TSP package not found on " + TSP_CONFIG_ZIP_CONTAINER);
                }
                
            } catch (IOException e) {
                checklistItem.setStatus(AIRLINE_STATUS_ERROR);  
                list.add(checklistItem);
                response.put(list, HttpStatus.INTERNAL_SERVER_ERROR);
                logger.error("Error retrieving TSP package: {}", e.getMessage(), e);
            }
        }
        else{
            checklistItem.setStatus(AIRLINE_STATUS_UNAUTHORIZED);  
            list.add(checklistItem);
            response.put(list, HttpStatus.UNAUTHORIZED);
            logger.error(NON_FDA_MEMBER, userGroup);
        }

        logger.info("TSP package check completed..");
        return CompletableFuture.completedFuture(response);
    }

    /**
	 * checkFlightPlan - gets the Flight Plan source-file status
	 * @param authToken access token
     * @param airline airline code
	 * @return status of flight plan source
     * @throws FileDownloadException 
	 */
    @Async("asyncExecutor")
	public CompletableFuture<Map<List<AirlineStatusChecklistItem>, HttpStatus>>checkFlightPlan(String authToken, String airline) throws FileDownloadException, InterruptedException {
        logger.info("Flight Plan source file check starting..");
        AirlineStatusChecklistItem checklistItem = new AirlineStatusChecklistItem();
        List<AirlineStatusChecklistItem> list = new ArrayList<>();
        Map<List<AirlineStatusChecklistItem>, HttpStatus> response = new LinkedHashMap<>();

        // Validate user
        final User user = azureADClientService.getUserInfoFromJwtAccessToken(authToken);
        List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
        if (airlineGroups.size() != 1) {
            throw new FileDownloadException(new ApiError("FILE_DOWNLOAD_FAILURE", "Failed to associate user with an airline", Constants.RequestFailureReason.UNAUTHORIZED));
        }
        String userGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);
        logger.info("User belongs to: {}", userGroup);

        if(userGroup.equals(USER_GROUP_AUTHORIZED)){
            String airlineGroup = airline.toLowerCase();

            CosmosDbFlightPlanSource flightPlanSource = new CosmosDbFlightPlanSource(airlineGroup);

            String fileName = airlineGroup + ".source";
            logger.debug("Retrieve data from [" + fileName + "]");

            checklistItem.setItem("Flight Plan");

            try {
                AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"), this.appProps.get("StorageKey"));
                try (ByteArrayOutputStream outputStream = asu.downloadFile(FLIGHT_PLAN_CONTAINER, fileName)) {

                    flightPlanSource.addFlightPlanSource(outputStream.toString());
                        
                    List<Object> content = new ArrayList<>();
                    content.add(flightPlanSource);

                    checklistItem.setContent(content);
                    checklistItem.setStatus(AIRLINE_STATUS_SUCCESS);
                    list.add(checklistItem);
                    response.put(list, HttpStatus.OK);
                    logger.info("Flight Plan setup is good");

                } catch (NullPointerException npe) {
                    checklistItem.setStatus(AIRLINE_STATUS_ERROR);
                    list.add(checklistItem);
                    response.put(list, HttpStatus.INTERNAL_SERVER_ERROR);
                    logger.error("Flight plan source [{}] not found on [{}]: {}", fileName, FLIGHT_PLAN_CONTAINER, npe.getMessage());
                } 
            }
            catch (IOException | org.apache.commons.configuration.ConfigurationException ioe) {
                checklistItem.setStatus(AIRLINE_STATUS_ERROR);
                list.add(checklistItem);
                response.put(list, HttpStatus.INTERNAL_SERVER_ERROR);
                logger.error("Failed to retrieve flight plan [{}] from [{}]: {}", fileName, FLIGHT_PLAN_CONTAINER, ioe.getMessage());
            }
        }
        else{
            checklistItem.setStatus(AIRLINE_STATUS_UNAUTHORIZED);
            list.add(checklistItem);
            response.put(list, HttpStatus.UNAUTHORIZED);
            logger.error(NON_FDA_MEMBER, userGroup);
           
        }
        
        logger.info("Flight Plan source check completed..");
        return CompletableFuture.completedFuture(response);
	}

    /**
	 * checkPreferences - retrieves the Airline Preferences
	 * @param authToken access token
     * @param airline airline code
	 * @return status of airline preferences
     * @throws InterruptedException
	 */
    @Async("asyncExecutor")
    public CompletableFuture<Map<List<AirlineStatusChecklistItem>, HttpStatus>>checkAirlinePreferences(String authToken, String airline) throws FileDownloadException, InterruptedException {
        logger.info("Airline Preferences check starting..");
        AirlineStatusChecklistItem checklistItem = new AirlineStatusChecklistItem();
        List<AirlineStatusChecklistItem> list = new ArrayList<>();
        Map<List<AirlineStatusChecklistItem>, HttpStatus> response = new LinkedHashMap<>();

        String userGroup = null;
        try {
            
            // Extract the access token from the authorization request header
            String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);
			
            // Get group membership of user issuing request. Ensure that user belongs to role-airlinefocal group
			// and one and only one airline group.
			User airlineFocalCurrentUser = azureADClientService.getUserInfoFromJwtAccessToken(accessTokenInRequest);

			// Validate user privileges by checking group membership. Must belong to Role-AirlineFocal group and a single Airline group.
			List<Group> airlineGroups = airlineFocalCurrentUser.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).peek(g -> logger.info("Airline Group: {}", g)).collect(Collectors.toList());
			if (airlineGroups.size() != 1 ) {
                logger.error("Error: {}", new ApiError("AIRLINE_PREFERENCES_FAILED", "User membership is ambiguous, airlines[" + airlineGroups.size() + "]", RequestFailureReason.UNAUTHORIZED));
				return null;
			} else {
				userGroup = airlineGroups.get(0).getDisplayName();
			}
            
            userGroup = userGroup.replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);

            if(userGroup.equals(USER_GROUP_AUTHORIZED)){

                StringBuilder airlineGroup = new StringBuilder("airline-");
                airlineGroup.append(airline.toLowerCase());
                
                airline = airlineGroup.toString();

                List<AirlinePreferences> airlinePreferences = featureManagementDao.getAirlinePreferences(airline, true);
                
                logger.info("Connecting to FDAGroundServicesDB..");
                logger.info("Airline preferences [{}]", airlinePreferences);
                logger.info("Returned [{}] airline preferences records of Airline Group [{}] from SQL DB", airlinePreferences.size(), airline);
                
                checklistItem.setItem("Airline Preferences");
                List<Object> description = new ArrayList<>();
                for (AirlinePreferences airlineField : airlinePreferences) {
                    airlineField.setAirline(airline);
                }

                description.add(airlinePreferences);
                checklistItem.setContent(description);
                checklistItem.setStatus(AIRLINE_STATUS_SUCCESS);
                list.add(checklistItem);
                response.put(list, HttpStatus.OK);

            }
            else{
                checklistItem.setStatus(AIRLINE_STATUS_UNAUTHORIZED);
                list.add(checklistItem);
                response.put(list, HttpStatus.UNAUTHORIZED);
                logger.error(NON_FDA_MEMBER, userGroup);
            }

        } catch (FeatureManagementException fme) {
            logger.error("FeatureManagementException: {}", fme.getMessage(), fme);
            checklistItem.setStatus(AIRLINE_STATUS_ERROR);
            list.add(checklistItem);
            response.put(list, HttpStatus.INTERNAL_SERVER_ERROR);
        } 

        logger.info("Airline Preferences check completed..");
        return CompletableFuture.completedFuture(response);
    }
    
}
