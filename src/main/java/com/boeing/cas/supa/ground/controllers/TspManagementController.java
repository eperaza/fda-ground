package com.boeing.cas.supa.ground.controllers;

import com.boeing.cas.supa.ground.exceptions.FileDownloadException;
import com.boeing.cas.supa.ground.pojos.Tsp;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.services.AircraftPropertyService;
import com.boeing.cas.supa.ground.services.AzureADClientService;
import com.boeing.cas.supa.ground.services.FileManagementService;
import com.boeing.cas.supa.ground.services.TspManagementService;
import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.boeing.cas.supa.ground.utils.Constants;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
@RequestMapping(path="/tsp")
public class TspManagementController {
	private static final String DefaultAircraftType = "B737-800";
    private final Logger logger = LoggerFactory.getLogger(TspManagementController.class);

    @Autowired
	private Map<String, String> appProps;

    @Autowired
    ServletContext context;

    @Autowired
    private TspManagementService tspManagementService;

    @Autowired
    private FileManagementService fileManagementService;

    @Autowired
    private AircraftPropertyService aircraftPropertyService;

    @Autowired
	private AzureADClientService aadClient;

    @RequestMapping(path="/getTspList", method = { RequestMethod.GET })
    public ResponseEntity<Object> getTspList(@RequestHeader("Authorization") String authToken, 
    		@RequestHeader(name = "airline", required = true) String airlineName,
    		@RequestHeader(name = "tail", required = true) String tailNumber) {

        logger.debug("TSPList for airline: " + airlineName);
        logger.debug("TSPList for tailNumber: " + tailNumber);

        List<Tsp> result = tspManagementService.getTsps(airlineName, tailNumber);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(path="/getActiveTsp", method = { RequestMethod.GET })
    public ResponseEntity<Object> getActiveTsp(@RequestHeader("Authorization") String authToken, 
    		@RequestHeader(name = "airline", required = true) String airlineName,
    		@RequestHeader(name = "tail", required = true) String tailNumber) {

        Object result = tspManagementService.getActiveTspByAirlineAndTailNumber(airlineName, tailNumber);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(path="/saveTsp", method = { RequestMethod.POST })
    public ResponseEntity<Object> saveTsp(@RequestBody String tspContent, 
    		@RequestHeader("Authorization") String authToken,
    		@RequestHeader(name = "airline", required = true) String airlineName,
    		@RequestHeader(name = "aircraftType", required = false, defaultValue = DefaultAircraftType) String aircraftType,
    		@RequestHeader(name = "cutoffDate", required = false) String cutoffDate,
    		@RequestHeader(name = "numberOfFlights", required = false) Integer numberOfFlights,
    		@RequestHeader(name = "active", required = false, defaultValue = "false") String active) {

        String userId = getUserId(authToken);

        boolean result = tspManagementService.saveTsp(airlineName, aircraftType, tspContent, userId, 
        		Boolean.TRUE.toString().equalsIgnoreCase(active), cutoffDate, numberOfFlights);

        if (result == false) {
        	 return new ResponseEntity<>("Failed to save TSP to database", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>("success", HttpStatus.OK);
    }
    
    @RequestMapping(path="/activateTsp", method = { RequestMethod.POST })
    public ResponseEntity<Object> activateTsp(@RequestHeader("Authorization") String authToken,
    		@RequestHeader(name = "airline", required = true) String airlineName,
    		@RequestHeader(name = "tail", required = true) String tailNumber, 
    		@RequestHeader(name = "version", required = true) String version) {

    	String userId = getUserId(authToken);

        boolean result = tspManagementService.activateTsp(airlineName, tailNumber, version, userId);

        if (result == false) {
            return new ResponseEntity<>("Failed to activate TSP to database", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>("success", HttpStatus.OK);
    }
    
    @RequestMapping(path = "/migrateTspFiles", method = {RequestMethod.GET})
    public ResponseEntity<String> migrateTspFiles(@RequestHeader("Authorization") String authToken) {
    	
        String userId = getUserId(authToken);
        
        try {
	        AzureStorageUtil util = new AzureStorageUtil(this.appProps.get("StorageAccountName"), this.appProps.get("StorageKey"));
			List<String> blobNames = util.getAllBlobNamesInTheContainer("tsp", null);
			for (String fileName: blobNames) {
				ByteArrayOutputStream outputStream = util.downloadFile("tsp", fileName);
				String airlineName = fileName.substring(0, fileName.indexOf("/"));
				String content = new String(outputStream.toByteArray());
				tspManagementService.saveTsp(airlineName, DefaultAircraftType, content, userId, true, null, null);
			}
        } catch (Exception ex) {
        	return new ResponseEntity<>("fail", HttpStatus.INTERNAL_SERVER_ERROR); 
        }

        return new ResponseEntity<>("success", HttpStatus.OK);
    }

    @RequestMapping(path="/getTspFromBlob", method = {RequestMethod.GET}, produces="application/zip")
    public void getTsp(HttpServletResponse response, @RequestHeader("Authorization") String authToken,
                       @RequestHeader("tailNumber") String tailNumber) throws FileDownloadException, IOException {
        String type = "tsp";
        String fileName = tailNumber + ".json";

        logger.debug("fetching TSP for:  " + tailNumber);

        byte[] tspFile = fileManagementService.getFileFromStorage(fileName, type, authToken);
        logger.debug("got TSP File");

        List<Tsp> tspList = tspManagementService.getTsps("AMX");

        // HARDCODED TAIL FOR NOW
        String aircraftProp = aircraftPropertyService.getAircraftProperty(authToken, "N342AM");

        HttpHeaders header = new HttpHeaders();
        header.add("Content-Disposition", "attachment; filename=tsp-test.zip");

        ZipOutputStream zipOutStream = new ZipOutputStream(response.getOutputStream());

        // tsp zipping
        zipOutStream.putNextEntry(new ZipEntry(fileName));
        InputStream inputStream = new ByteArrayInputStream(tspFile);
        IOUtils.copy(inputStream, zipOutStream);
        inputStream.close();

        // aircraft prop zipping
        zipOutStream.putNextEntry(new ZipEntry("aircraft.properties.json"));
        InputStream apropStream = new ByteArrayInputStream(aircraftProp.getBytes());
        IOUtils.copy(apropStream, zipOutStream);
        apropStream.close();

        zipOutStream.closeEntry();
        zipOutStream.close();
    }
    
    private String getUserId(String authToken) {
        // Extract the access token from the authorization request header
        String accessTokenInRequest = authToken.replace(Constants.AUTH_HEADER_PREFIX, StringUtils.EMPTY);
    	User user = null;
    	try {
    		user = aadClient.getUserInfoFromJwtAccessToken(accessTokenInRequest);
    	} catch (Exception ex) {}
    	
    	return user == null ? "SYSTEM" : user.getObjectId(); //TODO: Validate user to have permission before continue
    }
}
