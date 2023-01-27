package com.boeing.cas.supa.ground.controllers;

import java.io.IOException;
import java.util.List;

import com.boeing.cas.supa.ground.exceptions.OnsCertificateException;
import com.boeing.cas.supa.ground.exceptions.SupaSystemLogException;
import com.boeing.cas.supa.ground.pojos.FlightCount;
import com.boeing.cas.supa.ground.pojos.OnsCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
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
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.boeing.cas.supa.ground.utils.ControllerUtils;

@RestController
public class FlightRecordController {

	private final Logger logger = LoggerFactory.getLogger(FlightRecordController.class);

	@Autowired
	private FileManagementService fileManagementService;

	@RequestMapping(path = "/uploadLiteRecord", method = { RequestMethod.POST })
	public ResponseEntity<Object> uploadLiteRecord(final @RequestParam("file") MultipartFile flightRecord,
			@RequestHeader("Authorization") String authToken) throws FlightRecordException, IOException {
		FileManagementMessage flightRecordUploadResponse = null;
		try {
			if (flightRecord.isEmpty()) {
				logger.warn("The flight record payload is empty");
				throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPLOAD_FAILURE",
						"Empty or invalid file submitted", RequestFailureReason.BAD_REQUEST));
			}
			flightRecordUploadResponse = this.fileManagementService.uploadLiteRecord(flightRecord, authToken);
			return new ResponseEntity<>(flightRecordUploadResponse, HttpStatus.OK);
		} catch (FlightRecordException fre) {
			logger.error("Upload flight record failed: {}", fre.getMessage());
			return new ResponseEntity<>(fre.getError(),
					ControllerUtils.translateRequestFailureReasonToHttpErrorCode(fre.getError().getFailureReason()));
		}

	}

	@RequestMapping(path = "/uploadFlightRecord", method = { RequestMethod.POST })
	public ResponseEntity<Object> uploadFlightRecord(final @RequestParam("file") MultipartFile uploadFlightRecord,
			@RequestHeader("Authorization") String authToken) {

		try {

			if (uploadFlightRecord.isEmpty()) {
				logger.warn("The flight record payload is empty");
				throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPLOAD_FAILURE", "Empty or invalid file submitted", RequestFailureReason.BAD_REQUEST));
			}

			FileManagementMessage flightRecordUploadResponse = this.fileManagementService.uploadFlightRecord(uploadFlightRecord, authToken);
			return new ResponseEntity<>(flightRecordUploadResponse, HttpStatus.OK);
		} catch (FlightRecordException fre) {
			logger.error("Upload flight record failed: {}", fre.getMessage());
			return new ResponseEntity<>(fre.getError(), ControllerUtils.translateRequestFailureReasonToHttpErrorCode(fre.getError().getFailureReason()));
		}
	}

	@RequestMapping(path = "/uploadPilotNote", method = { RequestMethod.POST })
	public ResponseEntity<Object> uploadPilotNote(final @RequestParam("file") MultipartFile uploadPilotNote,
													 @RequestHeader("Authorization") String authToken) {

		try {

			if (uploadPilotNote.isEmpty()) {
				logger.warn("The flight record payload is empty");
				throw new SupaSystemLogException(new ApiError("FLIGHT_RECORD_UPLOAD_FAILURE", "Empty or invalid file submitted", RequestFailureReason.BAD_REQUEST));
			}

			FileManagementMessage pilotNoteUploadResponse = this.fileManagementService.uploadPilotNote(uploadPilotNote, authToken);
			return new ResponseEntity<>(pilotNoteUploadResponse, HttpStatus.OK);
		} catch (SupaSystemLogException fre) {
			logger.error("Upload pilot note failed: {}", fre.getMessage());
			return new ResponseEntity<>(fre.getError(), ControllerUtils.translateRequestFailureReasonToHttpErrorCode(fre.getError().getFailureReason()));
		}
	}

	@RequestMapping(path = "/listFlightRecordsLocation", method = { RequestMethod.GET }, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> listFlightRecordsLocation(@RequestHeader("Authorization") String authToken) {

		try {
			List<FileManagementMessage> fileMgmtMessages = this.fileManagementService.listFlightRecords(authToken);
			return new ResponseEntity<>(fileMgmtMessages, HttpStatus.OK);
		} catch (FlightRecordException fre) {
			return new ResponseEntity<>(new ApiError("FLIGHT_RECORD_LIST", fre.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@RequestMapping(path = "/uploadSupaSystemLog", method = { RequestMethod.POST })
	public ResponseEntity<Object> uploadSupaSystemLog(final @RequestParam("file") MultipartFile uploadSupaSystemLog,
													 @RequestHeader("Authorization") String authToken) {
		try {

			if (uploadSupaSystemLog.isEmpty()) {
				logger.warn("The flight log payload is empty");
				throw new SupaSystemLogException(new ApiError("SUPA_SYSTEM_LOG_UPLOAD_FAILURE", "Empty or invalid file submitted", RequestFailureReason.BAD_REQUEST));
			}

			FileManagementMessage supaSystemLogUploadResponse = this.fileManagementService.uploadSupaSystemLog(uploadSupaSystemLog, authToken);
			return new ResponseEntity<>(supaSystemLogUploadResponse, HttpStatus.OK);
		} catch (SupaSystemLogException fre) {
			logger.error("Upload flight log failed: {}", fre.getMessage());
			return new ResponseEntity<>(fre.getError(), ControllerUtils.translateRequestFailureReasonToHttpErrorCode(fre.getError().getFailureReason()));
		}
	}

	@RequestMapping(path = "/updateFlightRecordStatusOnAid", method = { RequestMethod.POST },
			consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> updateFlightRecordStatusOnAid(@RequestBody List<String> flightRecordNames,
			@RequestHeader("Authorization") String authToken) {

		try {

			if (CollectionUtils.isEmpty(flightRecordNames)) {
				throw new FlightRecordException(new ApiError("FLIGHT_RECORD_STATUS_FAILURE", "Flight record(s) are not specified", RequestFailureReason.BAD_REQUEST));
			}

			List<FileManagementMessage> fileMgmtMessages = this.fileManagementService.updateFlightRecordOnAidStatus(flightRecordNames, authToken);
			return new ResponseEntity<>(fileMgmtMessages, HttpStatus.OK);
		} catch (FlightRecordException fre) {
			return new ResponseEntity<>(new ApiError("FLIGHT_RECORD_STATUS_UPDATE", fre.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(path = "/listFlightRecords", method = { RequestMethod.GET }, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> listFlightRecords(@RequestHeader("Authorization") String authToken) {

		try {
			List<FileManagementMessage> fileMgmtMessages = this.fileManagementService.listFlightRecords(authToken);
			return new ResponseEntity<>(fileMgmtMessages, HttpStatus.OK);
		} catch (FlightRecordException fre) {
			return new ResponseEntity<>(new ApiError("FLIGHT_RECORD_LIST", fre.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@RequestMapping(path = "/getOnsCertInfo", method = {RequestMethod.GET} , produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> getOnsCertInfo(@RequestHeader("Authorization") String authToken) {

		try {
			List<OnsCertificate> certificates = this.fileManagementService.getOnsCertInfo(authToken);
			return new ResponseEntity<>(certificates, HttpStatus.OK);
		} catch (OnsCertificateException ons) {
			return new ResponseEntity<>(new ApiError("ONS_CERTIFICATE_FAILURE", ons.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}


	@RequestMapping(path = "/countFlightRecords", method = { RequestMethod.GET }, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> countFlightRecords(@RequestHeader("Authorization") String authToken) {

		try {
			List<FlightCount> flightCount = this.fileManagementService.countFlightRecords(authToken);
			return new ResponseEntity<>(flightCount, HttpStatus.OK);
		} catch (FlightRecordException fre) {
			return new ResponseEntity<>(new ApiError("FLIGHT_RECORD_COUNT", fre.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(path = "/getStatusOfFlightRecords", method = { RequestMethod.POST },
			consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
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
