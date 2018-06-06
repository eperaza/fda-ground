package com.boeing.cas.supa.ground.services;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.boeing.cas.supa.ground.dao.FlightRecordDao;
import com.boeing.cas.supa.ground.exceptions.FlightRecordException;
import com.boeing.cas.supa.ground.pojos.FlightRecord;
import com.boeing.cas.supa.ground.pojos.Group;
import com.boeing.cas.supa.ground.pojos.UploadFlightRecordMessage;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.ControllerUtils;

@Service
public class FileUploadService {

	private final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

	@Autowired
	private Map<String, String> appProps;

	@Autowired
	private AzureADClientService aadClient;

	@Autowired
	private FlightRecordDao flightRecordDao;

	public UploadFlightRecordMessage uploadFlightRecord(final MultipartFile uploadFlightRecord, String authToken) throws FlightRecordException {

		final UploadFlightRecordMessage flightRecordUploadResponse = new UploadFlightRecordMessage(uploadFlightRecord.getOriginalFilename());

		// Query database for an uploaded flight record matching the flight record name.
        String flightRecordName = uploadFlightRecord.getOriginalFilename();
		FlightRecord _flightRecord = this.flightRecordDao.getFlightRecord(flightRecordName);
		// If found, respond with the status of deleted-on-AID for the flight record.
		if (_flightRecord != null) {

			flightRecordUploadResponse.setUploaded(true);
			flightRecordUploadResponse.setDeletedOnAid(_flightRecord.isDeletedOnAid());
			logger.warn("Flight record {} already uploaded", ControllerUtils.sanitizeString(flightRecordName));
			return flightRecordUploadResponse;
		}

		// Determine the airline from the user's membership.
		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			throw new FlightRecordException("Failed to associate user with an airline");
		}
		String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);
		// Sample Flight Record name: FDA_ABCDEF_FDA101_KSEA_KORD_20180531_132001Z_004159
		List<String> flightRecordNameTokens = Arrays.asList(flightRecordName.split("_"));
        String uploadFolder = null;
        Path _uploadPath = null;
        long _fileSizeKb = 0L;
        String _airline = null,
        	   _tail = null,
        	   _flightDate = null,
        	   _flightTime = null,
        	   _storagePath = null;
        Instant _flightDatetime = null;
		try {
			_airline = flightRecordNameTokens.get(0);
			// Remove hyphen from tail identifier, if present, and convert to lowercase.
			_tail = flightRecordNameTokens.get(1).toLowerCase().replace("-", StringUtils.EMPTY);
			_flightDate = flightRecordNameTokens.get(5);
			_flightTime = flightRecordNameTokens.get(6);
			if (!_airline.equalsIgnoreCase(airlineGroup)) {
				throw new FlightRecordException(String.format("Flight record filename prefix %s is not associated with airline code %s", _airline, airlineGroup));
			}
		    _flightDatetime = OffsetDateTime.parse(String.format("%s_%s", _flightDate, _flightTime), Constants.FlightRecordDateTimeFormatterForParse).toInstant();
			_storagePath = new StringBuilder(_airline).append('/').append(_tail).append('/').append(OffsetDateTime.ofInstant(_flightDatetime, ZoneId.of("UTC")).format(Constants.FlightRecordDateTimeFormatterForFormat)).toString();

		    // Store the uploaded file in TEMP, in preparation for uploading to Azure and ADW
        	uploadFolder = ControllerUtils.saveUploadedFiles(Arrays.asList(uploadFlightRecord));
        	if (StringUtils.isBlank(uploadFolder)) {
        		throw new IOException("Failed to establish upload folder");
        	}
    		_uploadPath = Paths.get(new StringBuilder(uploadFolder)
    				.append(File.separator)
    				.append(uploadFlightRecord.getOriginalFilename())
    				.toString());
    		try (FileChannel fileChannel = FileChannel.open(_uploadPath)) {
        		_fileSizeKb = fileChannel.size();
    		}
		} catch (IndexOutOfBoundsException ioobe) {
			throw new FlightRecordException("Failed to extract identifying tokens from flight record filename");
		} catch (DateTimeParseException dtpe) {
			throw new FlightRecordException("Invalid date and time format in flight record filename");
		} catch (IOException ioe) {
			throw new FlightRecordException(String.format("I/O exception encountered %s", ioe.getMessage()));
		}

		final long fileSizeKb = _fileSizeKb;
        final String airline = _airline,
         	   storagePath = _storagePath;
		final Path uploadPath = _uploadPath;
        final Instant flightDatetime = _flightDatetime;

        // Prepare a final FlightRecord instance to:
        // (a) pass to the individual upload threads, so individual attributes can be updates as warranted.
        // (b) persist the flight record, and status, to the database
		final FlightRecord flightRecord = new FlightRecord(uploadFlightRecord.getOriginalFilename(), storagePath,
				fileSizeKb, flightDatetime, null, airline, user.getUserPrincipalName(), null, false, false, false);

		// Set up executor pool for performing ADW and Azure uploads concurrently
		ExecutorService es = Executors.newFixedThreadPool(3);
		List<Future<Boolean>> futures = new ArrayList<>();
		final Map<String, String> properties = this.appProps;

//		// ------- Adding file to ADW -------
//		logger.debug("Adding file to ADW");
//		Future<Boolean> adwFuture = es.submit(new Callable<Boolean>() {
//
//			@Override
//			public Boolean call() throws Exception {
//
//				Boolean xfr = false;
//				try {
//
//					logger.info("Starting ADW Transfer");
//					String host = properties.get("adwHost");
//					String usr = properties.get("adwUser");
//					String pwd = properties.get("adwPwd");
//					String path = properties.get("adwPath");
//					ADWTransferUtil adw = new ADWTransferUtil(host, usr, pwd, path);
//					logger.info(uploadPath.toFile().getAbsolutePath());
//					xfr = adw.sendFile(uploadPath.toFile().getAbsolutePath());
//					// Update ADW update status in flight record
//					flightRecord.setUploadToAdw(xfr);
//					logger.info("Transfer to ADW complete: {}", xfr);
//				}
//				catch(Exception e) {
//					logger.error("ApiError in ADW XFER: {}", e.getMessage(), e);
//				}
//
//				return xfr;
//			} 
//		});
//		futures.add(adwFuture);

		// ------- Adding file to Azure Storage and Message Queue -------
		logger.debug("Adding file to Azure Storage and Message Queue");
		Future<Boolean> azureFuture = es.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {

				Boolean upload = false;
				try {

					logger.info("Starting Azure upload");
					AzureStorageUtil asu = new AzureStorageUtil(properties.get("StorageAccountName"), properties.get("StorageKey"));
					upload = asu.uploadFlightRecord("flight-records", new StringBuilder(storagePath).append('/').append(flightRecord.getFlightRecordName()).toString(), uploadPath.toFile().getAbsolutePath(), user);
					logger.info("Upload to Azure complete: {}", upload);
				} catch (FlightRecordException fre) {
					logger.error("Failed to upload to Azure Storage: {}", fre.getMessage());
					flightRecordUploadResponse.setMessage(fre.getMessage());
				} catch(Exception e) {
					logger.error("ApiError in Azure upload: {}", e.getMessage(), e);
				}

				return upload;
			} 
		});
		futures.add(azureFuture);

		boolean adwBool = false;
		boolean azureBool = false;
		try {

//			adwBool = adwFuture.get();
			azureBool = azureFuture.get();
		} catch (InterruptedException | ExecutionException e) {
			logger.error("ApiError in running executionservice: {}", e.getMessage(), e);
		}
		es.shutdown();

		try {
			if (!es.awaitTermination(1, TimeUnit.MINUTES)) {
				es.shutdownNow();
			} 
		} catch (InterruptedException e) {

			Thread.currentThread().interrupt();
			logger.error("ApiError in shuttingdown executionservice: {}", e.getMessage(), e);
			es.shutdownNow();
		}

		if (!adwBool) {
			logger.warn("Failed to upload flight record {} to ADW", ControllerUtils.sanitizeString(flightRecordName));
		}

		if (!azureBool) {
			return flightRecordUploadResponse;
		}

		// if azure upload is success, record upload into database
		try {
	        this.flightRecordDao.insertFlightData(flightRecord);
			flightRecordUploadResponse.setUploaded(true);
		} catch (FlightRecordException fre) {
			logger.error("Failed to record flight record upload to FDA Ground! {}", fre.getMessage());
			flightRecordUploadResponse.setMessage(fre.getMessage());
		} catch (Exception e) {
			logger.error("Failed to record flight record upload to FDA Ground! {}", ExceptionUtils.getRootCause(e).getMessage());
			flightRecordUploadResponse.setMessage(ExceptionUtils.getRootCause(e).getMessage());
		}
        
		return flightRecordUploadResponse;
	}
}
