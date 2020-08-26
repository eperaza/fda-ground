package com.boeing.cas.supa.ground.services;

import com.boeing.cas.supa.ground.dao.FlightRecordDao;
import com.boeing.cas.supa.ground.exceptions.*;
import com.boeing.cas.supa.ground.pojos.*;
import com.boeing.cas.supa.ground.utils.*;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileManagementService {

	private final Logger logger = LoggerFactory.getLogger(FileManagementService.class);

	private final static String FLIGHT_RECORDS_STORAGE_CONTAINER = "flight-records";
	private final static String SUPA_SYSTEM_LOGS_STORAGE_CONTAINER = "supa-system-logs";
	public final static String TSP_CONFIG_ZIP_CONTAINER = "aircraft-config-package";
	private final static String TSP_STORAGE_CONTAINER = "tsp";
	private final static String MOBILECONFIG_STORAGE_CONTAINER = "config";
	private final static String CERTIFICATES_STORAGE_CONTAINER = "certificates";

	@Autowired
	private Map<String, String> appProps;

	@Autowired
	private AzureADClientService aadClient;

	@Autowired
	private AircraftPropertyService aircraftPropertyService;

	@Autowired
	private FlightRecordDao flightRecordDao;

	public List<String> getTspListFromStorage(String authToken) throws FileDownloadException, IOException {

		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			throw new FileDownloadException(new ApiError("FILE_DOWNLOAD_FAILURE", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED));
		}
		List<Group> roleGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_USER_ROLE_PREFIX)).collect(Collectors.toList());
		if (roleGroups.size() != 1) {
			throw new FileDownloadException(new ApiError("FILE_DOWNLOAD_FAILURE", "Failed to associate user with a role", RequestFailureReason.UNAUTHORIZED));
		}

		String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);

		AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"), this.appProps.get("StorageKey"));

		String container = TSP_STORAGE_CONTAINER;
		String filePath = new StringBuilder(container).append("/").append(airlineGroup.toUpperCase()).toString();

		if(asu.blobContainerExists(filePath)){
			throw new FileDownloadException(new ApiError("FILE_DOWNLOAD_FAILURE", "No TSP Blob exists for that airline", RequestFailureReason.BAD_REQUEST));
		}

		List<String> tspList = asu.getFilenamesFromBlob(container, airlineGroup);
		return tspList;

	}

	public byte[] zipFileList(List<String> tspList, String authToken) throws FileDownloadException, IOException {
		String container = TSP_STORAGE_CONTAINER;

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		BufferedOutputStream bufferOutputStream = new BufferedOutputStream(byteArrayOutputStream);
		ZipOutputStream zipOutputStream = new ZipOutputStream(bufferOutputStream);

		for(String tspFileName : tspList){
			byte[] tspJsonFile = this.getFileFromStorage(tspFileName, container, authToken);
			zipOutputStream.putNextEntry(new ZipEntry(tspFileName));
			InputStream inputStream = new ByteArrayInputStream(tspJsonFile);
			IOUtils.copy(inputStream, zipOutputStream);
			inputStream.close();

			int endStringIndex = tspFileName.length() - 4;
			String tailNo = new StringBuilder(tspFileName.substring(0, endStringIndex)).toString();

			String aircraftProp = aircraftPropertyService.getAircraftProperty(authToken, tailNo);
			if(aircraftProp != null){
				String aircraftPropFileName = new StringBuilder(tailNo).append("properties.json").toString();
				zipOutputStream.putNextEntry(new ZipEntry(aircraftPropFileName));
				InputStream apropStream = new ByteArrayInputStream(aircraftProp.getBytes());
				IOUtils.copy(apropStream, zipOutputStream);
				apropStream.close();
			}
			zipOutputStream.closeEntry();
		}
		zipOutputStream.close();
		byte[] zipFile = byteArrayOutputStream.toByteArray();
		logger.debug("FINISHED WRITING ZIP FILE");
		return zipFile;
	}

	public byte[] getFileFromStorage(String file, String type, String authToken) throws FileDownloadException {

		byte[] fileInBytes = new byte[0];

		try {

			// Determine the airline from the user's membership; this is specifically for TSP files.
			final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
			List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
			if (airlineGroups.size() != 1) {
				throw new FileDownloadException(new ApiError("FILE_DOWNLOAD_FAILURE", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED));
			}
			List<Group> roleGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_USER_ROLE_PREFIX)).collect(Collectors.toList());
			if (roleGroups.size() != 1) {
				throw new FileDownloadException(new ApiError("FILE_DOWNLOAD_FAILURE", "Failed to associate user with a role", RequestFailureReason.UNAUTHORIZED));
			}

			String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);

			AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"), this.appProps.get("StorageKey"));
			if (StringUtils.isBlank(file) || StringUtils.isBlank(type)) {
				throw new FileDownloadException(new ApiError("FILE_DOWNLOAD_FAILURE", "Missing or invalid file and/or type", RequestFailureReason.BAD_REQUEST));
			}

			// Resolve the container and file path based on the input arguments.
			String container = null, filePath = null;
			// If TSP, then prepend airline as virtual directory to file path
			if (TSP_STORAGE_CONTAINER.equals(type)) {
				container = TSP_STORAGE_CONTAINER;
				filePath = new StringBuilder(airlineGroup.toUpperCase()).append('/').append(file).toString();
				if (asu.blobExistsOnCloud(container, filePath) != true) {
					// in case the file is not exist, try to upper case the file name
					String tailNumberPart = file.substring(0, file.indexOf(".json"));
					filePath = new StringBuilder(airlineGroup.toUpperCase()).append('/').append(tailNumberPart.toUpperCase()).append(".json").toString();
					
					if (asu.blobExistsOnCloud(container, filePath) != true) {
						// in case the file is not exist, try to upper case the first letter to support camel file name
						filePath = new StringBuilder(airlineGroup.toUpperCase()).append('/')
													.append(file.substring(0, 1).toUpperCase())
													.append(file.substring(1)).toString();
					}
				}
			} else if (MOBILECONFIG_STORAGE_CONTAINER.equals(type)) {
				container = MOBILECONFIG_STORAGE_CONTAINER;
				filePath = file;
			} else {
				throw new FileDownloadException(new ApiError("FILE_DOWNLOAD_FAILURE", "Invalid file type requested for download", RequestFailureReason.BAD_REQUEST));
			}

			// Once container and file path are established, retrieve the file contents in bytes
			try (ByteArrayOutputStream outputStream = asu.downloadFile(container, filePath)) {

				if (outputStream == null) {
					throw new FileDownloadException(new ApiError("FILE_DOWNLOAD_FAILURE", String.format("No file corresponding to specified name %s and type %s", file, type), RequestFailureReason.NOT_FOUND));
				}
				outputStream.flush();
				fileInBytes = outputStream.toByteArray();

			} catch (IOException e) {
				logger.error("Error retrieving file [{}] of type [{}]: {}", ControllerUtils.sanitizeString(file), ControllerUtils.sanitizeString(type), e.getMessage(), e);
				throw new FileDownloadException(new ApiError("FILE_DOWNLOAD_FAILURE", String.format("Error retrieving file [%s] of type [%s]: %s", file, type, e.getMessage()), RequestFailureReason.INTERNAL_SERVER_ERROR));
			}
		} catch (IOException ioe) {
			logger.error("Error accessing Azure Storage: {}", ioe.getMessage(), ioe);
			throw new FileDownloadException(new ApiError("FILE_DOWNLOAD_FAILURE", "Error retrieving File Storage", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

		return fileInBytes;
	}

	public List<OnsCertificate> getOnsCertInfo(String authToken) throws OnsCertificateException {

		byte[] fileInBytes = new byte[0];
		List<OnsCertificate> certificates = new ArrayList<>();

		try {
			// Determine the airline from the user's membership; this is specifically for TSP files.
			final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
			List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
			if (airlineGroups.size() != 1) {
				throw new OnsCertificateException(new ApiError("FILE_DOWNLOAD_FAILURE", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED));
			}
			List<Group> roleGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_USER_ROLE_PREFIX)).collect(Collectors.toList());
			if (roleGroups.size() != 1) {
				throw new OnsCertificateException(new ApiError("FILE_DOWNLOAD_FAILURE", "Failed to associate user with a role", RequestFailureReason.UNAUTHORIZED));
			}

			String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);
			String userRole = roleGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_USER_ROLE_PREFIX, StringUtils.EMPTY);

			AzureStorageUtil asu = new AzureStorageUtil(this.appProps.get("StorageAccountName"), this.appProps.get("StorageKey"));
			logger.debug("user role equals [{}]", userRole);
			String file = "unknown";
			String role = "na";
			if (userRole.equalsIgnoreCase("airlinepilot")) { role = "fc"; file = "fc_exp2022_04_15.pfx"; }
			if (userRole.equalsIgnoreCase("airlinefocal")) { role = "fc"; file = "fc_exp2022_04_15.pfx"; }
			if (userRole.equalsIgnoreCase("airlineefbadmin")) { role = "fc"; file = "fc_exp2022_04_15.pfx"; }
			if (userRole.equalsIgnoreCase("airlinemaintenance")) { role = "mc"; file = "mc_exp2022_04_14.pfx"; }
			if (userRole.equalsIgnoreCase("airlinecheckairman")) { role = "mc"; file = "mc_exp2022_04_14.pfx"; }
			String filePath = new StringBuilder(airlineGroup.toUpperCase()).append('/').append(role)
					.append('/').append(file).toString();
			logger.debug("filename equals [{}]", filePath);

			try (ByteArrayOutputStream outputStream = asu.downloadFile(CERTIFICATES_STORAGE_CONTAINER, filePath)) {

				if (outputStream == null) {
					throw new OnsCertificateException(new ApiError("FILE_DOWNLOAD_FAILURE", String.format("No file corresponding to specified name %s", file), RequestFailureReason.NOT_FOUND));
				}
				outputStream.flush();
				fileInBytes = outputStream.toByteArray();

			} catch (IOException e) {
				logger.error("Error retrieving Ons Certificate file [{}]: {}", ControllerUtils.sanitizeString(file), e.getMessage(), e);
				throw new OnsCertificateException(new ApiError("FILE_DOWNLOAD_FAILURE", String.format("Error retrieving Ons Certificate file [%s]: %s", file, e.getMessage()), RequestFailureReason.INTERNAL_SERVER_ERROR));
			}
		} catch (IOException ioe) {
			logger.error("Error accessing Azure Storage: {}", ioe.getMessage(), ioe);
			throw new OnsCertificateException(new ApiError("FILE_DOWNLOAD_FAILURE", "Error retrieving Ons Certificate", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
		String key = "p@ssword";

		String base64EncodedPayload = Base64.getEncoder().encodeToString(key.getBytes());
		OnsCertificate onsCertificate = new OnsCertificate(base64EncodedPayload, fileInBytes);
		certificates.add(onsCertificate);
		return certificates;
	}


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
			throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPLOAD_FAILURE", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED));
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
				throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPLOAD_FAILURE", String.format("Flight record filename prefix %s is not associated with %s airline user", _airline, airlineGroup.toUpperCase()), RequestFailureReason.UNAUTHORIZED));
			}
			try {
			    _flightDatetime = OffsetDateTime.parse(String.format("%s_%s", _flightDate, _flightTime), Constants.FlightRecordDateTimeFormatterForParse).toInstant();
			} catch (DateTimeParseException dtpe) {
				// Treat as non-critical error... don't re-throw exception
			}
			
			// If tail and/or date-time tokens are missing or invalid, save file in UNRESOLVED folder
			if (StringUtils.isBlank(_tail) || _flightDatetime == null) {
				_storagePath = new StringBuilder(_airline).append("/UNRESOLVED").toString();
			} else {
				_storagePath = new StringBuilder(_airline).append('/').append(_tail).append('/').append(OffsetDateTime.ofInstant(_flightDatetime, ZoneId.of("UTC")).format(Constants.FlightRecordDateTimeFormatterForFormat)).toString();
			}

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
			throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPLOAD_FAILURE", "Failed to extract identifying tokens from flight record filename", RequestFailureReason.BAD_REQUEST));
		} catch (IOException ioe) {
			throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPLOAD_FAILURE", String.format("I/O exception encountered %s", ioe.getMessage()), RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

		final long fileSizeKb = _fileSizeKb;
        final String airline = _airline,
         	   storagePath = _storagePath;
		final Path uploadPath = _uploadPath;
		// If flight date and time is absent in flight record name, default it to the minimum supported Instant
        final Instant flightDatetime = _flightDatetime != null ? _flightDatetime : OffsetDateTime.parse("19700101_000000Z", Constants.FlightRecordDateTimeFormatterForParse).toInstant();

        final String aid_id = flightRecordDao.getLatestSupaVersion(airline, _airline + "/" + _tail + "/");

        // Prepare a final FlightRecord instance to:
        // (a) pass to the individual upload threads, so individual attributes can be updates as warranted.
        // (b) persist the flight record, and status, to the database
		final FlightRecord flightRecord = new FlightRecord(uploadFlightRecord.getOriginalFilename(), storagePath,
				fileSizeKb, flightDatetime, aid_id, airline, user.getUserPrincipalName(),
				null, false, false, false, null, null);

		// Set up executor pool for performing ADW and Azure uploads concurrently
		ExecutorService es = Executors.newFixedThreadPool(3);
		List<Future<Boolean>> futures = new ArrayList<>();
		final Map<String, String> properties = this.appProps;

		Future<Boolean> adwFuture = null;
		// Suppress ADW upload (set via application properties, typically false for non-Production environments)
		if (Boolean.parseBoolean(this.appProps.get("adwUpload"))) {

			// If ADW upload is set to true, then check user associated airline:
			// - non-FDA, proceed with upload
			// - FDA, proceed with upload only if adwUploadFDA is set to true
			if (!airlineGroup.equalsIgnoreCase("FDA")
				|| Boolean.parseBoolean(this.appProps.get("adwUploadFDA"))) {

				// ------- Adding file to ADW -------
				logger.debug("Adding file to ADW");
				adwFuture = es.submit(new Callable<Boolean>() {

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
			}
		}

		// ------- Adding file to Azure Storage and Message Queue -------
		logger.debug("Adding file to Azure Storage and Message Queue");
		Future<Boolean> azureFuture = es.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {

				Boolean upload = false;
				try {

					logger.info("Starting Azure upload");
					AzureStorageUtil asu = new AzureStorageUtil(properties.get("StorageAccountName"), properties.get("StorageKey"));
					upload = asu.uploadFlightRecord(FLIGHT_RECORDS_STORAGE_CONTAINER, new StringBuilder(storagePath).append('/').append(flightRecord.getFlightRecordName()).toString(), uploadPath.toFile().getAbsolutePath(), user);
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

			adwBool = adwFuture != null ? adwFuture.get() : false;
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
			throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPLOAD_FAILURE", flightRecordUploadResponse.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

		// Since Azure Storage upload is successful, the upload must be logged into the database
		try {
	        this.flightRecordDao.insertFlightData(flightRecord);
			flightRecordUploadResponse.setUploaded(true);
		} catch (FlightRecordException fre) {
			logger.error("Failed to record flight record upload to FDA Ground! {}", fre.getMessage());
			throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPLOAD_FAILURE", String.format("Failed to log flight record upload to FDA Ground: %s", fre.getMessage()), RequestFailureReason.INTERNAL_SERVER_ERROR));
		} catch (Exception e) {
			logger.error("Failed to record flight record upload to FDA Ground! {}", ExceptionUtils.getRootCause(e).getMessage());
			throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPLOAD_FAILURE", String.format("Failed to log flight record upload to FDA Ground: %s", ExceptionUtils.getRootCause(e).getMessage()), RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
        
		return flightRecordUploadResponse;
	}

	public FileManagementMessage uploadTspConfigPackage(byte[] zipFile, String fileName, String authToken) throws TspConfigLogException, IOException {
		logger.debug("Uploading package: " + fileName);

		final Map<String, String> properties = this.appProps;
		final FileManagementMessage tspUploadResponse = new FileManagementMessage(fileName);

		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			throw new TspConfigLogException(new ApiError("TSP_CONFIG_PACKAGE_UPLOAD_FAILURE", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED));
		}
		String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);
		ExecutorService es = Executors.newFixedThreadPool(3);
		List<Future<Boolean>> futures = new ArrayList<>();

		fileName = new StringBuilder(airlineGroup).append("-config-pkg.zip").toString();

		String uploadFolder = null;
		Path _uploadPath = null;
		long _fileSizeKb = 0L;
		String _storagePath = null;

		try {
				_storagePath = new StringBuilder(airlineGroup).toString();

			// Store the uploaded file in TEMP, in preparation for uploading to Azure and ADW
			uploadFolder = ControllerUtils.saveUploadedZip(zipFile, fileName);
			if (StringUtils.isBlank(uploadFolder)) {
				throw new IOException("Failed to establish upload folder");
			}
			_uploadPath = Paths.get(new StringBuilder(uploadFolder)
					.append(File.separator)
					.append(fileName)
					.toString());
			try (FileChannel fileChannel = FileChannel.open(_uploadPath)) {
				_fileSizeKb = fileChannel.size();
			}
		} catch (IndexOutOfBoundsException ioobe) {
			throw new TspConfigLogException(new ApiError("TSP_CONFIG_PACKAGE_UPLOAD_FAILURE", "Failed to extract identifying tokens from flight record filename", RequestFailureReason.BAD_REQUEST));
		} catch (IOException ioe) {
			throw new TspConfigLogException(new ApiError("TSP_CONFIG_PACKAGE_UPLOAD_FAILURE", String.format("I/O exception encountered %s", ioe.getMessage()), RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

		final String storagePath = _storagePath;
		final Path uploadPath = _uploadPath;
		final String filePath = new StringBuilder(storagePath).append('/').append(fileName).toString();
		// ------- Adding file to Azure Storage -------
		logger.debug("Adding file to Azure Storage");
		Future<Boolean> azureFuture = es.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {

				Boolean upload = false;
				try {
					logger.info("Starting Azure upload");
					AzureStorageUtil asu = new AzureStorageUtil(properties.get("StorageAccountName"), properties.get("StorageKey"));
					upload = asu.uploadTspZipConfig(TSP_CONFIG_ZIP_CONTAINER, filePath, uploadPath.toFile().getAbsolutePath());
					logger.info("Upload to Azure complete: {}", upload);
				} catch (TspConfigLogException ex) {
					logger.error("Failed to upload to Azure Storage: {}", ex.getMessage());
					tspUploadResponse.setMessage(ex.getMessage());
				} catch(Exception e) {
					logger.error("ApiError in Azure upload: {}", e.getMessage(), e);
				}
				return upload;
			}
		});
		futures.add(azureFuture);
		boolean azureBool = false;
		try {
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
		// get last modified timestamp after upload
		AzureStorageUtil asu = new AzureStorageUtil(properties.get("StorageAccountName"), properties.get("StorageKey"));
		Date lastModified = asu.getLastModifiedTimeStampFromBlob(TSP_CONFIG_ZIP_CONTAINER, filePath);
		tspUploadResponse.setLastModified(lastModified);
		tspUploadResponse.setUploaded(true);

		return tspUploadResponse;
	}

	public Date getBlobLastModifiedTimeStamp(String containerName, String fileName) throws TspConfigLogException, IOException {
		final Map<String, String> properties = this.appProps;
		logger.info("Fetching Last Modified TimeStamp From Blob ... " + containerName);
		logger.info("fileName: " + fileName);
		try{
			AzureStorageUtil asu = new AzureStorageUtil(properties.get("StorageAccountName"), properties.get("StorageKey"));

			Date lastModified = asu.getLastModifiedTimeStampFromBlob(containerName, fileName);

			return lastModified;
		}catch(IOException ex){
			throw new TspConfigLogException(new ApiError("TSP_CONFIG_PACKAGE_GET_LAST_MODIFIED_FAILURE", "FAILED TO RETRIEVE UPDATED TIMESTAMP FROM BLOB", RequestFailureReason.UNAUTHORIZED));
		}
	}

	public FileManagementMessage uploadSupaSystemLog(final MultipartFile uploadSupaSystemLog, String authToken) throws SupaSystemLogException {

		logger.debug("Upload Supa system log request");
		final FileManagementMessage supaSystemLogUploadResponse = new FileManagementMessage(uploadSupaSystemLog.getOriginalFilename());

		String supaSystemLogName = uploadSupaSystemLog.getOriginalFilename();

		// Determine the airline from the user's membership.
		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			throw new SupaSystemLogException(new ApiError("SUPA_SYSTEM_LOG_UPLOAD_FAILURE", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED));
		}

		String airlineGroup = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);
		// Sample Flight log: {airline}_{tailNumber}_{webFBSerialNumber}_systemLog_{yyyyMMdd}_{HHmmss}Z.zip
		List<String> supaSystemLogNameTokens = Arrays.asList(supaSystemLogName.split("_"));
		String uploadFolder = null;
		Path _uploadPath = null;
		long _fileSizeKb = 0L;
		String _airline = null,
				_tail = null,
				_logDate = null,
				_logTime = null,
				_storagePath = null;
		Instant _logDatetime = null;
		try {
			_airline = supaSystemLogNameTokens.get(0);
			// Remove hyphen from tail identifier, if present, and convert to lowercase.
			_tail = supaSystemLogNameTokens.get(1).toLowerCase().replace("-", StringUtils.EMPTY);
			_logDate = supaSystemLogNameTokens.get(4);
			_logTime = supaSystemLogNameTokens.get(5).replace(".zip", StringUtils.EMPTY);
			if (!_airline.equalsIgnoreCase(airlineGroup)) {
				throw new SupaSystemLogException(new ApiError("SUPA_SYSTEM_LOG_UPLOAD_FAILURE", String.format("Supa system log filename prefix %s is not associated with %s airline user", _airline, airlineGroup.toUpperCase()), RequestFailureReason.UNAUTHORIZED));
			}
			try {
				_logDatetime = OffsetDateTime.parse(String.format("%s_%s", _logDate, _logTime), Constants.SupaSystemLogDateTimeFormatterForParse).toInstant();
			} catch (DateTimeParseException dtpe) {
				logger.warn(dtpe.getMessage());
				// Treat as non-critical error... don't re-throw exception
			}

			// If tail and/or date-time tokens are missing or invalid, save file in undetermined folder
			if (StringUtils.isBlank(_tail) || _logDatetime == null) {
				_storagePath = new StringBuilder(_airline).append("/UNDETERMINED").toString();
			} else {
				_storagePath = new StringBuilder(_airline).append('/').append(_tail).append('/').append(OffsetDateTime.ofInstant(_logDatetime, ZoneId.of("UTC")).format(Constants.SupaSystemLogDateTimeFormatterForFormat)).toString();
			}

			// Store the uploaded file in TEMP, in preparation for uploading to Azure
			uploadFolder = ControllerUtils.saveUploadedFiles(Arrays.asList(uploadSupaSystemLog));
			if (StringUtils.isBlank(uploadFolder)) {
				throw new IOException("Failed to establish upload folder");
			}
			_uploadPath = Paths.get(new StringBuilder(uploadFolder)
					.append(File.separator)
					.append(uploadSupaSystemLog.getOriginalFilename())
					.toString());
			try (FileChannel fileChannel = FileChannel.open(_uploadPath)) {
				_fileSizeKb = fileChannel.size();
			}
		} catch (IndexOutOfBoundsException | IOException ioe) {
			throw new SupaSystemLogException(new ApiError("SUPA_SYSTEM_LOG_UPLOAD_FAILURE", "Failed to extract identifying tokens from flight log filename", RequestFailureReason.BAD_REQUEST));
		}

		final long fileSizeKb = _fileSizeKb;
		final String airline = _airline,
				storagePath = _storagePath;
		final Path uploadPath = _uploadPath;
		// If flight date and time is absent in flight record name, default it to the minimum supported Instant
		final Instant logDatetime = _logDatetime != null ? _logDatetime : OffsetDateTime.parse("19700101_000000Z", Constants.SupaSystemLogDateTimeFormatterForParse).toInstant();

		// Prepare a final Flight Log instance to
		// pass to the individual upload threads, so individual attributes can be updates as warranted.
		final SupaSystemLog supaSystemLog = new SupaSystemLog(uploadSupaSystemLog.getOriginalFilename(), storagePath,
				fileSizeKb, logDatetime, airline, user.getUserPrincipalName(), null);

		// Check to see if the Blob already exists
		boolean bExists = false;
		try {
			AzureStorageUtil asu = new AzureStorageUtil(appProps.get("StorageAccountName"), appProps.get("StorageKey"));
			bExists = asu.blobExistsOnCloud(SUPA_SYSTEM_LOGS_STORAGE_CONTAINER, new StringBuilder(storagePath).append('/')
					.append(supaSystemLog.getSupaSystemLogName()).toString());
		} catch (IOException ioe) {

		} 
		
		if (bExists) {
			throw new SupaSystemLogException(new ApiError("SUPA_SYSTEM_LOG_UPLOAD_FAILURE", "The specified blob already exists.", RequestFailureReason.ALREADY_REPORTED));
		}
		// Set up executor pool for performing ADW and Azure uploads concurrently
		ExecutorService es = Executors.newFixedThreadPool(3);
		List<Future<Boolean>> futures = new ArrayList<>();
		final Map<String, String> properties = this.appProps;

		// ------- Adding file to Azure Storage -------
		logger.debug("Adding file to Azure Storage");
		Future<Boolean> azureFuture = es.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {

				Boolean upload = false;
				try {
					logger.info("Starting Azure upload");
					AzureStorageUtil asu = new AzureStorageUtil(properties.get("StorageAccountName"), properties.get("StorageKey"));
					upload = asu.uploadSupaSystemLog(SUPA_SYSTEM_LOGS_STORAGE_CONTAINER, new StringBuilder(storagePath).append('/')
						.append(supaSystemLog.getSupaSystemLogName()).toString(), uploadPath.toFile().getAbsolutePath());
					logger.info("Upload to Azure complete: {}", upload);
				} catch (SupaSystemLogException fre) {
					logger.error("Failed to upload to Azure Storage: {}", fre.getMessage());
					supaSystemLogUploadResponse.setMessage(fre.getMessage());
				} catch(Exception e) {
					logger.error("ApiError in Azure upload: {}", e.getMessage(), e);
				}
				return upload;
			}
		});
		futures.add(azureFuture);

		boolean azureBool = false;
		try {
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

		// If Azure Storage upload fails, that is a crucial error that warrants an appropriate error response to the user
		if (!azureBool) {
			throw new SupaSystemLogException(new ApiError("SUPA_SYSTEM_LOG_UPLOAD_FAILURE",
				supaSystemLogUploadResponse.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
		supaSystemLogUploadResponse.setUploaded(true);

		return supaSystemLogUploadResponse;
	}


	public List<FileManagementMessage> updateFlightRecordOnAidStatus(List<String> flightRecordNames, String authToken) throws FlightRecordException {

		// Ensure that the user is authorized to update the status, by comparing the user-airline
		// with the airline token in the flight record name.
		List<FileManagementMessage> fileMgmtMessages = new ArrayList<>();

		// Determine the airline from the user's membership.
		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPDATE_STATUS_FAILURE", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED));
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
						throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPDATE_STATUS_FAILURE", String.format("Flight record filename prefix %s is not associated with airline code %s", _airline, airlineGroup), RequestFailureReason.UNAUTHORIZED));
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
							throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPDATE_STATUS_FAILURE", "Flight record not found", RequestFailureReason.NOT_FOUND));
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
			throw new FlightRecordException(new ApiError("FLIGHT_RECORD_RETRIEVAL_FAILURE", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED));
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


	public List<FlightCount> countFlightRecords(String authToken) throws FlightRecordException {

		// Determine the airline from the user's membership.
		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			throw new FlightRecordException(new ApiError("FLIGHT_RECORD_RETRIEVAL_FAILURE", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED));
		}
		String airline = airlineGroups.get(0).getDisplayName().replace(Constants.AAD_GROUP_AIRLINE_PREFIX, StringUtils.EMPTY);

		List<FlightCount> flightCountList = this.flightRecordDao.getAllFlightCounts(airline);
		return flightCountList;
	}


	public List<FileManagementMessage> getStatusOfFlightRecords(List<String>flightRecordNames, String authToken) throws FlightRecordException {
		
		List<FileManagementMessage> listOfFlightMgmtMessages = new ArrayList<>();

		// Determine the airline from the user's membership.
		final User user = aadClient.getUserInfoFromJwtAccessToken(authToken);
		List<Group> airlineGroups = user.getGroups().stream().filter(g -> g.getDisplayName().toLowerCase().startsWith(Constants.AAD_GROUP_AIRLINE_PREFIX)).collect(Collectors.toList());
		if (airlineGroups.size() != 1) {
			throw new FlightRecordException(new ApiError("FLIGHT_RECORD_STATUS_FAILURE", "Failed to associate user with an airline", RequestFailureReason.UNAUTHORIZED));
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