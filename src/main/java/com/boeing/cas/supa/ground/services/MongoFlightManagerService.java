package com.boeing.cas.supa.ground.services;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.CosmosDbFlightPlanSource;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class MongoFlightManagerService {

	private final Logger logger = LoggerFactory.getLogger(MongoFlightManagerService.class);


	@Autowired
	private Map<String, String> appProps;

    public Object getAllFlightObjectsFromCosmosDB(Optional<String> flightId, Optional<String> departureAirport,
                                                   Optional<String> arrivalAirport, CosmosDbFlightPlanSource source)
    {
        Map<String, String> query = new HashMap<String, String>();

        logger.debug("get flights for [{}]", source.getAirline());
        if (flightId.isPresent() || departureAirport.isPresent() || arrivalAirport.isPresent()) {

            if (flightId.isPresent()) {
                query.put("flightId", flightId.get());
                logger.info("flightId [{}]", flightId.get());
            } else {
                // parameter not passed
                logger.info("flightId not present");
            }

            if (departureAirport.isPresent()) {
                query.put("departureAirport", departureAirport.get());
                logger.info("departureAirport [{}]", departureAirport.get());
            } else {
                // parameter not passed
                logger.info("departureAirport not present");
            }

            if (arrivalAirport.isPresent()) {
                query.put("arrivalAirport", arrivalAirport.get());
                logger.info("arrivalAirport [{}]", arrivalAirport.get());
            } else {
                // parameter not passed
                logger.info("arrivalAirport not present");
            }
        }

        try {
            return searchGeneric(query, source);
        }
        catch (Exception ex)
        {
            logger.error("Request all flight objects from CosmosDB failed: {}", ex.getMessage(), ex);
            return new ApiError("FLIGHT_OBJECTS_REQUEST", ex.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR);
        }
    }


    public Object getFlightObjectByIdFromCosmosDB(String id, CosmosDbFlightPlanSource source)
    {
        logger.debug("get flights by id for [{}]", id);

        try {
            return searchid(id, source);

        } catch (Exception ex) {
            logger.error("Request flight object from CosmosDB failed: {}", ex.getMessage(), ex);
            return new ApiError("FLIGHT_PLANS_REQUEST", ex.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR);
        }

    }


    public Object getOperationalFlightPlanByFlightPlanIdFromCosmosDB(String flightPlanId, CosmosDbFlightPlanSource source)
    {
        Map<String, String> query = new HashMap<String, String>();
        if (flightPlanId != null && !flightPlanId.equals(""))
            query.put("flightPlanId", flightPlanId);

        logger.debug("get flight plans for [{}]", flightPlanId);

        try {
            return searchOFP(query, source);

        } catch (Exception ex) {
            logger.error("Request operational flight plan from CosmosDB failed: {}", ex.getMessage(), ex);
            return new ApiError("OPERATIONAL_FLIGHT_PLAN_REQUEST", ex.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR);
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
                .append(":10255/?ssl=true&replicaSet=globaldb");

        //logger.debug("ConnectionString=[" + cosmosDbUrl.toString() + "]");
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
            cursor = collection.find(searchQuery).projection(searchLabels).sort(sortLabels).iterator();

            while (cursor.hasNext()) {
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

    private Object searchid(String id, CosmosDbFlightPlanSource source) {

        Map<String, String> query = new HashMap<String, String>();
            query.put("id", id);

        Map<String, Integer> labels = new HashMap<String, Integer>();
        labels.put("id", 1);
        labels.put("flightPlanId", 1);
        labels.put("flightId", 1);
        labels.put("tail", 1);
        labels.put("estDepartureTime", 1);
        labels.put("departureAirport", 1);
        labels.put("arrivalAirport", 1);

        BasicDBObject searchLabels = new BasicDBObject();

        for (Map.Entry<String, Integer> entry : labels.entrySet()) {
            searchLabels.put(entry.getKey(), entry.getValue());
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
                .append(":10255/?ssl=true&replicaSet=globaldb");

        //logger.debug("ConnectionString=[" + cosmosDbUrl.toString() + "]");
        StringBuilder response = new StringBuilder();
        response.append("{\"data\":{\"perfectFlight\":{\"version\":0,\"id\":\"" + id + "\",\"flight\":{");

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

            cursor = collection.find(searchQuery).projection(searchLabels).iterator();
            StringBuffer sbFlightInfo = new StringBuffer();
            StringBuffer sbMetaData = new StringBuffer();

            while (cursor.hasNext()) {
                Document dbo = cursor.next();
                if (dbo.containsKey("_id")) {
                    dbo.remove("_id");
                }
                String[] tmp = dbo.toJson().split(",");

                for (String stmp : tmp) {
                    if (stmp.toLowerCase().trim().startsWith("\"tail")) sbFlightInfo.append(stmp + ",");
                    if (stmp.toLowerCase().trim().startsWith("\"flightid")) sbFlightInfo.append(stmp  + ",");
                    if (stmp.toLowerCase().trim().startsWith("\"flightplanid")) sbMetaData.append(stmp);
                }
                response.append(sbFlightInfo.toString() + "\"route\":" + dbo.toJson().replace("+00:00", ""));
            }
            // need to strip-off last comma, if there is one.
            if (response.toString().endsWith(",")) {
                response = new StringBuilder(response.toString().substring(0, response.toString().length() - 1));
            }
            // add metadata
            response.append(",\"metadata\":{" + sbMetaData + "}}}}}");

        }catch(NullPointerException ex){
            logger.debug("NullPointer Ex with Cursor or MongoClient: {}", ex.getMessage());
        } finally {
            cursor.close();
            mongoClient.close();
        }
        return response.toString();
    }


    private Object searchOFP(Map<String, String> query, CosmosDbFlightPlanSource source) {

        Map<String, Integer> labels = new HashMap<String, Integer>();
        labels.put("flightPlan", 1);

        BasicDBObject searchLabels = new BasicDBObject();

        for (Map.Entry<String, Integer> entry : labels.entrySet()) {
            searchLabels.put(entry.getKey(), entry.getValue());
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
            .append(":10255/?ssl=true&replicaSet=globaldb");

        //logger.debug("ConnectionString=[" + cosmosDbUrl.toString() + "]");
        StringBuilder response = new StringBuilder();
        response.append("{\"data\":{\"flightPlan\":{\"file\":");

        MongoClient mongoClient = null;
        MongoCursor<Document> cursor = null;
        StringBuffer sbtmp = new StringBuffer();
        try {
            MongoClientURI uri = new MongoClientURI(cosmosDbUrl.toString());
            mongoClient = new MongoClient(uri);
            MongoDatabase database = mongoClient.getDatabase(source.getDatabaseName());

            MongoCollection<Document> collection = database.getCollection(source.getCollectionName());
            BasicDBObject searchQuery = new BasicDBObject();

            for (Map.Entry<String, String> entry : query.entrySet()) {
                searchQuery.put(entry.getKey(), entry.getValue());
            }

            cursor = collection.find(searchQuery).projection(searchLabels).iterator();
            while (cursor.hasNext()) {
                Document dbo = cursor.next();
                sbtmp.append(dbo.toJson());
            }
        }
        catch(NullPointerException ex){
            logger.debug("NullPointer Ex with Cursor or MongoClient: {}", ex.getMessage());
        } finally {
            cursor.close();
            mongoClient.close();
        }
        int startHere = 0;
        startHere = sbtmp.toString().indexOf("\"flightPlan\" :");
        if (startHere > 0) {
            startHere += new String("\"flightPlan\" :").length();
            response.append(sbtmp.toString().substring(startHere));
        }
		response.append("}}");
		return response.toString();
	}
}
