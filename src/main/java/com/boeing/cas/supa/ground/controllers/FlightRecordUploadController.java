package com.boeing.cas.supa.ground.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.boeing.cas.supa.ground.exceptions.FlightRecordException;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.UploadFlightRecordMessage;
import com.boeing.cas.supa.ground.services.FileUploadService;

@RestController
public class FlightRecordUploadController {

	@Autowired
	private FileUploadService fileUploadService;

	@RequestMapping(path="/uploadFlightRecord", method = { RequestMethod.POST })
	public ResponseEntity<Object> uploadFlightRecord(final @RequestParam("file") MultipartFile uploadFlightRecord, @RequestHeader("Authorization") String authToken) {

		try {

	        if (uploadFlightRecord.isEmpty()) {
	        	throw new FlightRecordException("No file submitted");
			}

			UploadFlightRecordMessage flightRecordUploadResponse = fileUploadService.uploadFlightRecord(uploadFlightRecord, authToken);
			return new ResponseEntity<>(flightRecordUploadResponse, HttpStatus.OK);
		} catch (FlightRecordException fre) {
			return new ResponseEntity<>(new ApiError("FLIGHT_RECORD_UPLOAD", fre.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
