package com.boeing.cas.supa.ground.controllers;

import com.boeing.cas.supa.ground.exceptions.FileDownloadException;
import com.boeing.cas.supa.ground.exceptions.TspConfigLogException;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.FileManagementMessage;
import com.boeing.cas.supa.ground.pojos.Group;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.services.AircraftPropertyService;
import com.boeing.cas.supa.ground.services.AzureADClientService;
import com.boeing.cas.supa.ground.services.FileManagementService;
import com.boeing.cas.supa.ground.utils.CheckSumUtil;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.ControllerUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.boeing.cas.supa.ground.services.FileManagementService.TSP_CONFIG_ZIP_CONTAINER;

@Controller
public class AircraftPropertyController {

    private final Logger logger = LoggerFactory.getLogger(AircraftPropertyController.class);

    @Autowired
    private AircraftPropertyService aircraftPropertyService;

    @Autowired
    private AzureADClientService azureADClientService;

    @Autowired
    private CheckSumUtil checkSumUtil;

    @Autowired
    private FileManagementService fileManagementService;

    @RequestMapping(path="/getAircraftConfiguration", method={RequestMethod.GET}, produces="application/zip")
    public ResponseEntity<byte[]> getAircraftConfiguration(
                                         @RequestHeader("Authorization") String authToken,
                                         @RequestHeader(name = "lastUpdated", required = false) Date lastUpdated) throws IOException, NoSuchAlgorithmException, TspConfigLogException, FileDownloadException {

        String airlineName =  azureADClientService.validateAndGetAirlineName(authToken);
        final User user = azureADClientService.getUserInfoFromJwtAccessToken(authToken);
        List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
        if (airlineGroups.size() != 1) {
            throw new FileDownloadException(new ApiError("FILE_DOWNLOAD_FAILURE", "Failed to associate user with an airline", Constants.RequestFailureReason.UNAUTHORIZED));
        }
        String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);

        // file path name to retrieve from blob - it is not truly a real directory
        String fileName = new StringBuilder(airlineGroup).append("/").append(airlineGroup).append("-config-pkg.zip").toString();

        if(lastUpdated != null){
            logger.debug("DATE WAS PASSED IN!!!");
            Date lastModified = fileManagementService.getBlobLastModifiedTimeStamp(TSP_CONFIG_ZIP_CONTAINER, fileName);
            logger.debug("retrieved timestamp: " + lastModified.toString());

            // both dates are equal
            if(lastUpdated.compareTo(lastModified) == 0){
                // do nothing, return
                return new ResponseEntity<>(HttpStatus.OK);
            }else{
                // get TSP Config zip package
                byte[] zipFile = aircraftPropertyService.getAircraftConfig(authToken);
                // insert into DB
                FileManagementMessage zipUploadmsg = fileManagementService.uploadTspConfigPackage(zipFile, "test-aircraft-config.zip", authToken);
                String checkSum = checkSumUtil.generateCheckSum(zipFile);
                String lastModifiedStamp = zipUploadmsg.getLastModified().toString();

                HttpHeaders header = new HttpHeaders();
                header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
                header.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
                header.add("CheckSum", checkSum);
                header.add("lastModifiedDate", lastModifiedStamp);

                return new ResponseEntity<>(zipFile, header, HttpStatus.OK);
            }
        }
        // get TSP Config zip package
        byte[] zipFile = aircraftPropertyService.getAircraftConfig(authToken);
        // insert into DB
        FileManagementMessage zipUploadmsg = fileManagementService.uploadTspConfigPackage(zipFile, "test-aircraft-config.zip", authToken);
        String checkSum = checkSumUtil.generateCheckSum(zipFile);
        String lastModifiedStamp = zipUploadmsg.getLastModified().toString();

        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
        header.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
        header.add("CheckSum", checkSum);
        header.add("lastModifiedDate", lastModifiedStamp);

        return new ResponseEntity<>(zipFile, header, HttpStatus.OK);
    }

    @RequestMapping(path="/getAircraftProperty", method = { RequestMethod.GET })
    public ResponseEntity<Object> getAircraftProperty(@RequestHeader("Authorization") String authToken,
                                                      @RequestHeader(name = "tailNumber", required = true) String tailNumber) throws IOException {

        logger.debug("got to endpoint with tail: " + tailNumber);

        Object result = aircraftPropertyService.getAircraftProperty(authToken, tailNumber);

        logger.debug("aircraft prop: " + result);
        if (result instanceof ApiError) {
            return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(path="/getAircraftPropertiesByAirline", method={ RequestMethod.GET })
    public ResponseEntity<Object> getAircraftPropertiesByAirline(@RequestHeader("Authorization") String authToken){

        logger.debug("hit new getACProperties Endpoing %%%%%%% ");

        Object result = aircraftPropertyService.getAircraftPropertiesByAirline(authToken);
        if(result instanceof ApiError){
            return new ResponseEntity<>(result, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(((ApiError) result).getFailureReason()));
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}