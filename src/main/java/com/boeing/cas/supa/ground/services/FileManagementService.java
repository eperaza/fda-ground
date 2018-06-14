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
import com.boeing.cas.supa.ground.pojos.FileManagementMessage;
import com.boeing.cas.supa.ground.pojos.FlightRecord;
import com.boeing.cas.supa.ground.pojos.Group;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.utils.ADWTransferUtil;
import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.boeing.cas.supa.ground.utils.Constants;
import com.boeing.cas.supa.ground.utils.ControllerUtils;

@Service
public class FileManagementService {

	private final Logger logger = LoggerFactory.getLogger(FileManagementService.class);

	@Autowired
	private Map<String, String> appProps;

	@Autowired
	private AzureADClientService aadClient;

	@Autowired
	private FlightRecordDao flightRecordDao;

	public FileManagementMessage uploadFlightRecord(final MultipartFile uploadFlightRecord, String authToken) throws FlightRecordException {

		logger.debug("Upload flight record request received for processing");
		final FileManagementMessage flightRecordUploadResponse = new FileManagementMessage(uploadFlightRecord.getOriginalFilename());

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

		// ------- Adding file to ADW -------
		logger.debug("Adding file to ADW");
		Future<Boolean> adwFuture = es.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {

				Boolean xfr = false;
				try {

					logger.info("Starting ADW Transfer");
					String host = properties.get("adwHost");
					String usr = properties.get("adwUser");
					String pwd = properties.get("adwPwd");
					String path = properties.get("adwPath");
					ADWTransferUtil adw = new ADWTransferUtil(host, usr, pwd, path);
					xfr = adw.sendFile(uploadPath.toFile().getAbsolutePath());
					// Update ADW update status in flight record
					flightRecord.setUploadToAdw(xfr);
					logger.info("Transfer to ADW complete: {}", xfr);
				}
				catch(Exception e) {
					logger.error("ApiError in ADW XFER: {}", e.getMessage(), e);
				}

				return xfr;
			} 
		});
		futures.add(adwFuture);

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

			adwBool = adwFuture.get();
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

		// If ADW upload fails, logging a warning is sufficient (for now)
		if (!adwBool) {
			logger.warn("Failed to upload flight record {} to ADW", ControllerUtils.sanitizeString(flightRecordName));
		}

		// If Azure Storage upload fails, that is a crucial error that warrants an appropriate error response to the user
		if (!azureBool) {
			throw new FlightRecordException(flightRecordUploadResponse.getMessage());
		}

		// Since Azure Storage upload is successful, the upload must be logged into the database
		try {
	        this.flightRecordDao.insertFlightData(flightRecord);
			flightRecordUploadResponse.setUploaded(true);
		} catch (FlightRecordException fre) {
			logger.error("Failed to record flight record upload to FDA Ground! {}", fre.getMessage());
			throw new FlightRecordException(String.format("Failed to log flight record upload to FDA Ground: %s", fre.getMessage()));
		} catch (Exception e) {
			logger.error("Failed to record flight record upload to FDA Ground! {}", ExceptionUtils.getRootCause(e).getMessage());
			throw new FlightRecordException(String.format("Failed to log flight record upload to FDA Ground: %s", ExceptionUtils.getRootCause(e).getMessage()));
		}
        
		return flightRecordUploadResponse;
	}

	public List<FileManagementMessage> updateFlightRecordOnAidStatus(List<String> flightRecordNames, String authToken) throws FlightRecordException {

		// Ensure that the user is authorized to update the status, by comparing the user-airline
		// with the airline token in the flight record name.
		List<FileManagementMessage> fileMgmtMessages = new ArrayList<>();

		// Determine the airline from the user's membership.
		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			throw new FlightRecordException("Failed to associate user with an airline");
		}
		String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);

		fileMgmtMessages = flightRecordNames.parallelStream()
			.map(frn -> new FileManagementMessage(frn))
			.map(fmm -> {
				List<String> flightRecordNameTokens = Arrays.asList(fmm.getFlightRecordName().split("_"));
				String _airline = null;
				try {					
					_airline = flightRecordNameTokens.get(0);
					if (!_airline.equalsIgnoreCase(airlineGroup)) {
						throw new FlightRecordException(String.format("Flight record filename prefix %s is not associated with airline code %s", _airline, airlineGroup));
					}
				} catch (IndexOutOfBoundsException ioobe) {
					fmm.setMessage("Failed to extract identifying tokens from flight record filename");
				} catch (FlightRecordException fre) {
					fmm.setMessage(fre.getMessage());
				} catch (Exception e) {
					fmm.setMessage(e.getMessage());
				}
				return fmm;
			})
			.map(fmm -> {
				if (fmm.getMessage() == null) {
					try {
						this.flightRecordDao.updateFlightRecordOnAidStatus(fmm.getFlightRecordName());
					} catch (Exception e) {
						fmm.setMessage(ExceptionUtils.getRootCause(e).getMessage());
					}
				}
				return fmm;
			})
			.map(fmm -> {
				if (fmm.getMessage() == null) {
					// Retrieve the updated flight record
					try {
						
						FlightRecord flightRecord = this.flightRecordDao.getFlightRecord(fmm.getFlightRecordName());
						if (flightRecord == null) {
							throw new FlightRecordException("Flight record not found");
						} else if (!flightRecord.isDeletedOnAid()) {
							logger.error("Unable to update deleted-on-AID status on flight record: {}", flightRecord.toString());
							fmm.setUploaded(true);
							fmm.setMessage("Failed to update flight record's deleted-on-AID status");
						} else {
							fmm.setUploaded(true);
							fmm.setDeletedOnAid(true);
						}
					} catch (Exception e) {
						logger.error("Unable to retrieve or update deleted-on-AID status on flight record: {}", fmm.getFlightRecordName(), e.getMessage());
						fmm.setMessage(e.getMessage());
					}
				}

				return fmm;
			})
			.collect(Collectors.toList());

		return fileMgmtMessages;
	}

	public List<FileManagementMessage> listFlightRecords(String authToken) throws FlightRecordException {

		List<FileManagementMessage> listOfFlightMgmtMessages = new ArrayList<>();

		// Determine the airline from the user's membership.
		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			throw new FlightRecordException("Failed to associate user with an airline");
		}
		String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);

		List<FlightRecord> listOfFlightRecords = this.flightRecordDao.getAllFlightRecords(airlineGroup);
		if (listOfFlightRecords != null && listOfFlightRecords.size() > 0) {
			
			listOfFlightMgmtMessages = listOfFlightRecords
					.parallelStream()
					.map(fr -> new FileManagementMessage(true, fr.getFlightRecordName(), fr.isDeletedOnAid(), null))
					.collect(Collectors.toList());
		}

		return listOfFlightMgmtMessages;
	}
	
	public List<FileManagementMessage> getStatusOfFlightRecords(List<String>flightRecordNames, String authToken) throws FlightRecordException {
		
		List<FileManagementMessage> listOfFlightMgmtMessages = new ArrayList<>();

		// Determine the airline from the user's membership.
		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			throw new FlightRecordException("Failed to associate user with an airline");
		}
		String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);

		listOfFlightMgmtMessages = flightRecordNames.parallelStream()
			.filter(StringUtils::isNotBlank)
			.map(frn -> {
				if (!frn.startsWith(airlineGroup.toUpperCase())) {
					return new FileManagementMessage(false, frn, false, "Missing or invalid flight identifier");
				}
				try {
					FlightRecord flightRecord = this.flightRecordDao.getFlightRecord(frn);
					if (flightRecord == null) {
						return new FileManagementMessage(false, frn, false, "Missing or invalid flight identifier");
					} else {
						return new FileManagementMessage(true, frn, flightRecord.isDeletedOnAid(), null);
					}
				} catch (FlightRecordException fre) {
					return new FileManagementMessage(false, frn, false, fre.getMessage());
				} catch (Exception e) {
					return new FileManagementMessage(false, frn, false, e.getMessage());
				}
			})
			.collect(Collectors.toList());
		
		return listOfFlightMgmtMessages;
	}
}
