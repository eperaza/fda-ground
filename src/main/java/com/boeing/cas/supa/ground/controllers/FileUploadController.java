package com.boeing.cas.supa.ground.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.boeing.cas.supa.ground.helpers.HttpClientHelper;
import com.boeing.cas.supa.ground.pojos.UploadMessage;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.utils.ADWTransferUtil;
import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.boeing.cas.supa.ground.utils.Constants;

@RestController
public class FileUploadController {

	private final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

	@Autowired
	private Map<String, String> appProps;

	@RequestMapping(path="/uploadFile", method = { RequestMethod.POST })
	public ResponseEntity<Object> uploadFile(final @RequestParam("file") MultipartFile uploadfile, final HttpServletRequest httpRequest) {

		logger.debug("Upload File method invoked -  Single file upload!");

        if (uploadfile.isEmpty()) {

        	UploadMessage fileUploadMessage = new UploadMessage(Constants.FAILURE, Constants.FAILURE, "Empty File");
			return new ResponseEntity<>(fileUploadMessage, HttpStatus.BAD_REQUEST);
		}

        String uploadFolder = null;
        try {
        	uploadFolder = FileUploadController.saveUploadedFiles(Arrays.asList(uploadfile));
        	if (StringUtils.isBlank(uploadFolder)) {
        		throw new IOException("Failed to establish upload folder");
        	}
		} catch (IOException e) {
			UploadMessage fileUploadMessage = new UploadMessage(Constants.FAILURE, Constants.FAILURE, String.format("I/O Exception: %s", e.getMessage()));
			return new ResponseEntity<>(fileUploadMessage, HttpStatus.BAD_REQUEST);
		}

		final Path uploadFolderPath = Paths.get(uploadFolder + File.separator + uploadfile.getOriginalFilename());
		logger.debug("File uploaded to {}", uploadFolderPath.toFile().getAbsolutePath());
		
		// ------- Adding file to ADW -------
		logger.debug("Adding file to ADW");
		ExecutorService es = Executors.newFixedThreadPool(3);
		List<Future<Boolean>> futures = new ArrayList<>();
		final Map<String, String> properties = this.appProps;
		Future<Boolean> adwFuture = es.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {

				Boolean xfr = false;
				try {

					logger.debug("Starting ADW Transfer");
					String host = properties.get("adwHost");
					String usr = properties.get("adwUser");
					String pwd = properties.get("adwPwd");
					String path = properties.get("adwPath");
					ADWTransferUtil adw = new ADWTransferUtil(host, usr, pwd, path);
					xfr = adw.sendFile(uploadFolderPath.toFile().getAbsolutePath());
					logger.debug("Transfer to ADW complete: {}", xfr);
				}
				catch(Exception e) {
					logger.error("ApiError in ADW XFER: {}", e.getMessage(), e);
				}

				return xfr;
			} 
		});
		
		// ------- Adding file to Azure Storage and Message Queue -------
		logger.debug("Adding file to Azure Storage and Message Queue");
		Future<Boolean> azureFuture = es.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {

				User user = HttpClientHelper.getUserInfoFromHeader(properties.get("AzureADTenantName"), httpRequest);
				Boolean upload = false;
				try {

					logger.debug("Starting Azure upload");
					AzureStorageUtil asu = new AzureStorageUtil(properties.get("StorageAccountName"), properties.get("StorageKey"));
					upload = asu.uploadFile(uploadFolderPath.toFile().getAbsolutePath(), uploadfile.getOriginalFilename(), user);
					logger.debug("Upload to Azure complete: {}", upload);
				}
				catch(Exception e) {
					logger.error("ApiError in Azure upload: {}", e.getMessage(), e);
				}

				return upload;
			} 
		});
		
		futures.add(adwFuture);
		futures.add(azureFuture);

		boolean azureBool = false;
		boolean adwBool = false;

		try {

			logger.debug("Getting results for Azure and ADW uploads");
			azureBool = azureFuture.get();
			adwBool = adwFuture.get();
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

		UploadMessage fileUploadMessage = new UploadMessage(adwBool ? Constants.SUCCESS : Constants.FAILURE, azureBool ? Constants.SUCCESS : Constants.FAILURE, "Uploaded File: " +uploadfile.getOriginalFilename());
		return new ResponseEntity<>(fileUploadMessage, HttpStatus.OK);
	}

	private static String saveUploadedFiles(List<MultipartFile> files) throws IOException {

		Path tempDirPath = Files.createTempDirectory(StringUtils.EMPTY);
		String uploadFolder = tempDirPath.toString();

		for (MultipartFile file : files) {

			if (file.isEmpty()) {
				continue; // skip to next iteration
			}

			byte[] bytes = file.getBytes();
			Path path = Paths.get(uploadFolder + File.separator + file.getOriginalFilename());
			Files.write(path, bytes);
		}

		return uploadFolder;
	}
}
