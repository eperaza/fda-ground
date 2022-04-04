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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import com.boeing.cas.supa.ground.dao.FeatureManagementDao;
import com.boeing.cas.supa.ground.exceptions.FeatureManagementException;
import com.boeing.cas.supa.ground.exceptions.TspConfigLogException;
import com.boeing.cas.supa.ground.pojos.AirlinePreferences;
import com.boeing.cas.supa.ground.pojos.AirlineStatusChecklistItem;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.CosmosDbFlightPlanSource;
import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.configuration.ConfigurationException;
import org.bson.Document;

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
    private Map<String, String> appProps;

    @Autowired
    FileManagementService fileManagementService;

    @Autowired
    private FeatureManagementDao featureManagementDao;

    @Autowired
	private MongoFlightManagerService mongoFlightManagerService;

    private final Logger logger = LoggerFactory.getLogger(AirlineStatusService.class);
    private static final String FLIGHT_PLAN_CONTAINER = "flight-plan-source";
    
    /**
     * checkAutoConfig - gets the airline TSP package status
     * 
     * @param authToken access token
     * @param airline   airline code
     * @return airline TSP status
     * @throws IOException
     */
    @Async("asyncExecutor")
    public CompletableFuture<Map<List<AirlineStatusChecklistItem>, HttpStatus>> checkAutoConfig(String authToken,
            String airline) throws IOException, NoSuchAlgorithmException, TspConfigLogException,
            InterruptedException {
        logger.info("Auto Config package check starting..");
        AirlineStatusChecklistItem checklistItem = new AirlineStatusChecklistItem();
        List<AirlineStatusChecklistItem> list = new ArrayList<>();
        Map<List<AirlineStatusChecklistItem>, HttpStatus> response = new LinkedHashMap<>();
        Date lastModified = null;

        String airlineGroup = airline.toLowerCase();

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
                checklistItem.setStatus(HttpStatus.OK);
                list.add(checklistItem);

                response.put(list, HttpStatus.OK);
                logger.info("TSP package setup is good");

            } else {
                checklistItem.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
                list.add(checklistItem);
                response.put(list, HttpStatus.INTERNAL_SERVER_ERROR);
                logger.error("TSP package not found on " + TSP_CONFIG_ZIP_CONTAINER);
            }

        } catch (IOException e) {
            checklistItem.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            list.add(checklistItem);
            response.put(list, HttpStatus.INTERNAL_SERVER_ERROR);
            logger.error("Error retrieving TSP package: {}", e.getMessage(), e);
        }

        logger.info("TSP package check completed..");
        return CompletableFuture.completedFuture(response);
    }

    /**
     * checkFlightPlan - gets the Flight Plan source-file status
     * 
     * @param authToken access token
     * @param airline   airline code
     * @return status of flight plan source
     * @throws InterruptedException
     * @throws ConfigurationException
     * @throws JSONException

     */
    @Async("asyncExecutor")
    public CompletableFuture<Map<List<AirlineStatusChecklistItem>, HttpStatus>> checkFlightPlan(String authToken,
            String airline) throws InterruptedException, ConfigurationException {
        logger.info("Flight Plan source file check starting..");
        AirlineStatusChecklistItem checklistItem = new AirlineStatusChecklistItem();
        List<AirlineStatusChecklistItem> list = new ArrayList<>();
        Map<List<AirlineStatusChecklistItem>, HttpStatus> response = new LinkedHashMap<>();

        String airlineGroup = airline.toLowerCase();

        CosmosDbFlightPlanSource flightPlanSource = new CosmosDbFlightPlanSource(airlineGroup);
    
        String fileName = airlineGroup + ".source";
        logger.debug("Retrieve data from [" + fileName + "]");

        checklistItem.setItem("Flight Plan");

        try {
            AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"),
                    this.appProps.get("StorageKey"));
            try (ByteArrayOutputStream outputStream = asu.downloadFile(FLIGHT_PLAN_CONTAINER, fileName)) {

                flightPlanSource.addFlightPlanSource(outputStream.toString());

                logger.debug("getting the airline source " + flightPlanSource);

                Object flightObjects = null;

                if (flightPlanSource != null ) {
                    logger.debug("Use CosmosDb to obtain the flight plan.");
                    Optional<Integer> limit = Optional.of(new Integer(1));
                    Optional<String> flightId = Optional.empty();
                    Optional<String> departureAirport = Optional.empty();
                    Optional<String> arrivalAirport = Optional.empty();
                    flightObjects = mongoFlightManagerService.getAllFlightObjectsFromCosmosDB(flightId, departureAirport, arrivalAirport, flightPlanSource, limit);
                    logger.info("got flight objects..");
                }

                List<Object> content = new ArrayList<>();
                String x = flightObjects.toString().replace("{\"data\":{\"perfectFlights\":[", "[");
                x = x.replace("]}}", "]");

                ObjectMapper objectMapper = new ObjectMapper();

                //add flight plan source
                Map<String, Object> description = new HashMap<>();
                description.put("source", flightPlanSource);
                Object json = objectMapper.convertValue(description, Object.class);
                content.add(json);

                //add flight objects
                json = objectMapper.readValue(x, Object.class);
                description = new HashMap<>();
                description.put("sampleObject", json);
                json = objectMapper.convertValue(description, Object.class);
                content.add(json);

                checklistItem.setContent(content);
                checklistItem.setStatus(HttpStatus.OK);
                list.add(checklistItem);
                response.put(list, HttpStatus.OK);

                logger.info("Flight Plan setup is good");

            } catch (NullPointerException npe) {
                checklistItem.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
                list.add(checklistItem);
                response.put(list, HttpStatus.INTERNAL_SERVER_ERROR);
                logger.error("Flight plan source [{}] not found on [{}]: {}", fileName, FLIGHT_PLAN_CONTAINER,
                        npe.getMessage());
            }
        } catch (IOException ioe) {
            checklistItem.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            list.add(checklistItem);
            response.put(list, HttpStatus.INTERNAL_SERVER_ERROR);
            logger.error("Failed to retrieve flight plan [{}] from [{}]: {}", fileName, FLIGHT_PLAN_CONTAINER,
                    ioe.getMessage());
        }

        logger.info("Flight Plan source check completed..");
        return CompletableFuture.completedFuture(response);
    }

    /**
     * checkPreferences - retrieves the Airline Preferences
     * 
     * @param authToken access token
     * @param airline   airline code
     * @return status of airline preferences
     * @throws InterruptedException
     */
    @Async("asyncExecutor")
    public CompletableFuture<Map<List<AirlineStatusChecklistItem>, HttpStatus>> checkAirlinePreferences(
            String authToken, String airline) throws InterruptedException {
        logger.info("Airline Preferences check starting..");
        AirlineStatusChecklistItem checklistItem = new AirlineStatusChecklistItem();
        List<AirlineStatusChecklistItem> list = new ArrayList<>();
        Map<List<AirlineStatusChecklistItem>, HttpStatus> response = new LinkedHashMap<>();

        String userGroup = null;

        try {

            StringBuilder airlineGroup = new StringBuilder("airline-");
            airlineGroup.append(airline.toLowerCase());

            airline = airlineGroup.toString();

            List<AirlinePreferences> airlinePreferences = featureManagementDao.getAirlinePreferences(airline, true);

            logger.info("Connecting to FDAGroundServicesDB..");
            logger.info("Airline preferences [{}]", airlinePreferences);
            logger.info("Returned [{}] airline preferences records of Airline Group [{}] from SQL DB",
                    airlinePreferences.size(), airline);

            checklistItem.setItem("Airline Preferences");
            List<Object> description = new ArrayList<>();
            for (AirlinePreferences airlineField : airlinePreferences) {
                airlineField.setAirline(airline);
            }

            description.add(airlinePreferences);
            checklistItem.setContent(description);
            checklistItem.setStatus(HttpStatus.OK);
            list.add(checklistItem);
            response.put(list, HttpStatus.OK);

        } catch (FeatureManagementException fme) {
            logger.error("FeatureManagementException: {}", fme.getMessage(), fme);
            checklistItem.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            list.add(checklistItem);
            response.put(list, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        logger.info("Airline Preferences check completed..");
        return CompletableFuture.completedFuture(response);
    }

    public Object getAllFlightObjectsFromCosmosDB(CosmosDbFlightPlanSource source)
    {
        Map<String, String> query = new HashMap<String, String>();

        logger.debug("get flights for [{}]", source.getAirline());
    
        try {
            return searchGeneric(query, source);
        }
        catch (Exception ex)
        {
            logger.error("Request all flight objects from CosmosDB failed: {}", ex.getMessage(), ex);
            return new ApiError("FLIGHT_OBJECTS_REQUEST", ex.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR);
        }
    }

    private Object searchGeneric(Map<String, String> query, CosmosDbFlightPlanSource source) {

        Map<String, Integer> labels = new HashMap<String, Integer>();
        labels.put("id", 1);
        labels.put("flightPlanId", 1);
        labels.put("flightId", 1);
        labels.put("estDepartureTime", 1);
        labels.put("departureAirport", 1);
        labels.put("arrivalAirport", 1);
        labels.put("planCI", 1);
        labels.put("planRevNum", 1);

        Map<String, Integer> sortBy = new HashMap<>();
        sortBy.put("estDepartureTime", -1);

        BasicDBObject searchLabels = new BasicDBObject();
        BasicDBObject sortLabels = new BasicDBObject();

        for (Map.Entry<String, Integer> entry : labels.entrySet()) {
            searchLabels.put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Integer> entry : sortBy.entrySet()) {
            sortLabels.put(entry.getKey(), entry.getValue());
        }

        String password = source.getPrimaryPassword();
        if (password == null) {
            return new ApiError("FLIGHT_OBJECTS_REQUEST", "CosmosDb Primary Password missing for " + source.getAirline(), RequestFailureReason.INTERNAL_SERVER_ERROR);
        }

        StringBuilder cosmosDbUrl = new StringBuilder("mongodb://")
                .append(source.getUserName()).append(":")
                .append(password)
                .append("@")
                .append(source.getServerName())
                .append(":10255/?ssl=true&replicaSet=globaldb&retrywrites=false&maxIdleTimeMS=120000&appName=@")
                .append(source.getUserName())
                .append("@");

        logger.debug("ConnectionString=[" + cosmosDbUrl.toString() + "]");
        StringBuilder response = new StringBuilder();
        response.append("{\"data\":{\"perfectFlights\":[");

        MongoClient mongoClient = null;
        MongoCursor<Document> cursor = null;
        try {
            MongoClientURI uri = new MongoClientURI(cosmosDbUrl.toString());
            mongoClient = new MongoClient(uri);
            MongoDatabase database = mongoClient.getDatabase(source.getDatabaseName());

            MongoCollection<Document> collection = database.getCollection(source.getCollectionName());

            BasicDBObject searchQuery = new BasicDBObject();


            for (Map.Entry<String, String> entry : query.entrySet()) {
                searchQuery.put(entry.getKey(), entry.getValue());
            }

            logger.info("MongoDb: sort by estDepartureTime");
            //MongoCursor<Document> cursor = collection.find(searchQuery).projection(searchLabels).iterator();
            cursor = collection.find(searchQuery).projection(searchLabels).sort(sortLabels).limit(10).iterator();


            while (cursor.hasNext()) {
                //logger.debug("Found a doc");
                Document dbo = cursor.next();
                if (dbo.containsKey("_id")) {
                    dbo.remove("_id");
                }
                response.append(dbo.toJson().replace("+00:00", "") + ",");
            }

        } catch(NullPointerException ex){
            logger.debug("NullPointer Ex with Cursor or MongoClient: {}", ex.getMessage());
        } finally{
            cursor.close();
            mongoClient.close();
        }
        // need to strip-off last comma, if there is one.
        if (response.toString().endsWith(",")) {
            response = new StringBuilder(response.toString().substring(0, response.toString().length() - 1));
        }
		response.append("]}}");
		return response.toString();
	}

}
