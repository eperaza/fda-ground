package com.boeing.cas.supa.ground.controllers;

import com.boeing.cas.supa.ground.exceptions.TspConfigLogException;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.services.AircraftPropertyService;
import com.boeing.cas.supa.ground.services.AzureADClientService;
import com.boeing.cas.supa.ground.services.FileManagementService;
import com.boeing.cas.supa.ground.utils.CheckSumUtil;
import com.boeing.cas.supa.ground.utils.ControllerUtils;
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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Controller
public class AircraftPropertyController {

    @Autowired
    private AircraftPropertyService aircraftPropertyService;
    private final Logger logger = LoggerFactory.getLogger(AircraftPropertyController.class);

    @Autowired
    private AzureADClientService azureADClientService;

    @Autowired
    private CheckSumUtil checkSumUtil;

    @Autowired
    private FileManagementService fileManagementService;

    @RequestMapping(path="/getAircraftConfiguration", method={RequestMethod.GET}, produces="application/zip")
    public void getAircraftConfiguration(HttpServletResponse response,
                                         @RequestHeader("Authorization") String authToken,
                                         @RequestHeader(name = "lastUpdated", required = true) Date lastUpdated) throws IOException, NoSuchAlgorithmException, TspConfigLogException {

        String airlineName =  azureADClientService.validateAndGetAirlineName(authToken);

        // get TSP Config zip package
        byte[] zipFile = aircraftPropertyService.getAircraftConfig(authToken);
        // insert into DB
        fileManagementService.uploadTspConfigPackage(zipFile, "test-aircraft-config.zip", authToken);

        Date updatedDate = new Date();

        String checkSum = checkSumUtil.generateCheckSum(zipFile);
        String fileName = new StringBuilder(airlineName).append("-config.zip").toString();

        HttpHeaders header = new HttpHeaders();
        header.add("Content-Disposition", "attachment; filename=" + fileName);
        header.add("CheckSum", checkSum);

        OutputStream outStream = response.getOutputStream();
        outStream.write(zipFile);
        outStream.close();
        response.flushBuffer();
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
}