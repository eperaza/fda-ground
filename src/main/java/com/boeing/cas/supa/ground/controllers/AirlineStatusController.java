package com.boeing.cas.supa.ground.controllers;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.boeing.cas.supa.ground.exceptions.AirlineStatusUnauthorizedException;
import com.boeing.cas.supa.ground.exceptions.FileDownloadException;
import com.boeing.cas.supa.ground.exceptions.TspConfigLogException;
import com.boeing.cas.supa.ground.pojos.AirlineStatusChecklistItem;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.Group;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.services.AirlineStatusService;
import com.boeing.cas.supa.ground.services.AzureADClientService;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Component
@RequestMapping(path = "/airlineStatus")
public class AirlineStatusController {

    @Autowired
    AirlineStatusService service;

    @Autowired
    AzureADClientService aadClient;

    private final Logger logger = LoggerFactory.getLogger(AirlineStatusController.class);

    private final String[] ALLOWED_ROLES = {"role-airlineefbadmin", "role-airlinefocal"};

    @GetMapping(path = "/get")
    public ResponseEntity<Object> getStatusByAirline(
            @RequestParam String airline) throws NoSuchAlgorithmException, IOException,
            TspConfigLogException, FileDownloadException, InterruptedException, ExecutionException,
            CancellationException, CompletionException, AirlineStatusUnauthorizedException {

        List<AirlineStatusChecklistItem> checklist = new ArrayList<>();
        CompletableFuture<Map<List<AirlineStatusChecklistItem>, HttpStatus>> tspReport = null;
        CompletableFuture<Map<List<AirlineStatusChecklistItem>, HttpStatus>> flightPlanReport = null;
        CompletableFuture<Map<List<AirlineStatusChecklistItem>, HttpStatus>> preferencesReport = null;
        Object resultObj;

        try{

            // Auto Config package setup status
            tspReport = service.checkAutoConfig(airline);

            // Flight Plan setup status
            flightPlanReport = service.checkFlightPlan(airline);

            // Airline Preferences status
            preferencesReport = service.checkAirlinePreferences(airline);

            // Wait until all async services are done
            CompletableFuture.allOf(tspReport, flightPlanReport, preferencesReport).join();
            logger.info("asyncExecutor completed successfully");

            Map.Entry<List<AirlineStatusChecklistItem>, HttpStatus> tspEntry = tspReport.get().entrySet().iterator()
                    .next();
            List<AirlineStatusChecklistItem> tspList = tspEntry.getKey();
            HttpStatus tspStatus = tspEntry.getValue();

            Map.Entry<List<AirlineStatusChecklistItem>, HttpStatus> flightPlanEntry = flightPlanReport.get().entrySet()
                    .iterator().next();
            List<AirlineStatusChecklistItem> flightPlanList = flightPlanEntry.getKey();
            HttpStatus flightPlanStatus = flightPlanEntry.getValue();

            Map.Entry<List<AirlineStatusChecklistItem>, HttpStatus> preferencesEntry = preferencesReport.get()
                    .entrySet().iterator().next();
            List<AirlineStatusChecklistItem> preferencesList = preferencesEntry.getKey();
            HttpStatus preferencesStatus = preferencesEntry.getValue();

            if (tspStatus == HttpStatus.INTERNAL_SERVER_ERROR || flightPlanStatus == HttpStatus.INTERNAL_SERVER_ERROR
                    || preferencesStatus == HttpStatus.INTERNAL_SERVER_ERROR) {
                checklist.addAll(tspList);
                checklist.addAll(flightPlanList);
                checklist.addAll(preferencesList);
                return new ResponseEntity<>(checklist, HttpStatus.MULTI_STATUS);
            } else {
                checklist.addAll(tspList);
                checklist.addAll(flightPlanList);
                checklist.addAll(preferencesList);
                return new ResponseEntity<>(checklist, HttpStatus.OK);
            }

        } catch (InterruptedException ie) {
            logger.error("Process was interrupted: {}", ie.getMessage(), ie);
            resultObj = new ApiError("Process was interrupted", ie.getMessage(),
                    RequestFailureReason.INTERNAL_SERVER_ERROR);
            // Restore interrupted state...
            Thread.currentThread().interrupt();
            return new ResponseEntity<>(resultObj, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NullPointerException npe) {
            logger.error("Error generating report: {}", npe.getMessage(), npe);
            resultObj = new ApiError("Error generating report", npe.getMessage(),
                    RequestFailureReason.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>(resultObj, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (CompletionException ce) {
            logger.error("Process completion error: {}", ce.getMessage(), ce);
            resultObj = new ApiError("Process completion error", ce.getMessage(),
                    RequestFailureReason.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>(resultObj, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (CancellationException ce) {
            logger.error("Process was cancelled: {}", ce.getMessage(), ce);
            resultObj = new ApiError("Process was cancelled", ce.getMessage(),
                    RequestFailureReason.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>(resultObj, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

}
