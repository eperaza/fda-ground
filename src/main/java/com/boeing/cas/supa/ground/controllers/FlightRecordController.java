package com.boeing.cas.supa.ground.controllers;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.boeing.cas.supa.ground.exceptions.FlightRecordException;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.FileManagementMessage;
import com.boeing.cas.supa.ground.services.FileManagementService;

@RestController
public class FlightRecordController {

	@Autowired
	private FileManagementService fileManagementService;

	@RequestMapping(path = "/uploadFlightRecord", method = { RequestMethod.POST })
	public ResponseEntity<Object> uploadFlightRecord(final @RequestParam("file") MultipartFile uploadFlightRecord,
			@RequestHeader("Authorization") String authToken) {

		try {

			if (uploadFlightRecord.isEmpty()) {
				throw new FlightRecordException("No file submitted");
			}

			FileManagementMessage flightRecordUploadResponse = this.fileManagementService.uploadFlightRecord(uploadFlightRecord, authToken);
			return new ResponseEntity<>(flightRecordUploadResponse, HttpStatus.OK);
		} catch (FlightRecordException fre) {
			return new ResponseEntity<>(new ApiError("FLIGHT_RECORD_UPLOAD", fre.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(path = "/updateFlightRecordStatusOnAid", method = { RequestMethod.POST })
	public ResponseEntity<Object> updateFlightRecordStatusOnAid(
			@RequestParam("flightRecordName") String flightRecordName,
			@RequestHeader("Authorization") String authToken) {

		try {

			if (StringUtils.isBlank(flightRecordName)) {
				throw new FlightRecordException("Flight record is not specified");
			}

			FileManagementMessage fileMgmtMessage = this.fileManagementService.updateFlightRecordOnAidStatus(flightRecordName, authToken);
			return new ResponseEntity<>(fileMgmtMessage, HttpStatus.OK);
		} catch (FlightRecordException fre) {
			return new ResponseEntity<>(new ApiError("FLIGHT_RECORD_UPDATE", fre.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(path = "/listFlightRecords", method = { RequestMethod.GET })
	public ResponseEntity<Object> listFlightRecords(@RequestHeader("Authorization") String authToken) {

		try {
			List<FileManagementMessage> fileMgmtMessages = this.fileManagementService.listFlightRecords(authToken);
			return new ResponseEntity<>(fileMgmtMessages, HttpStatus.OK);
		} catch (FlightRecordException fre) {
			return new ResponseEntity<>(new ApiError("FLIGHT_RECORD_LIST", fre.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(path = "/getStatusOfFlightRecords", method = { RequestMethod.POST })
	public ResponseEntity<Object> getStatusOfFlightRecords(@RequestBody List<String> flightRecordNames,
			@RequestHeader("Authorization") String authToken) {

		try {
			List<FileManagementMessage> fileMgmtMessages = this.fileManagementService.getStatusOfFlightRecords(flightRecordNames, authToken);
			return new ResponseEntity<>(fileMgmtMessages, HttpStatus.OK);
		} catch (FlightRecordException fre) {
			return new ResponseEntity<>(new ApiError("FLIGHT_RECORDS_LIST", fre.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
