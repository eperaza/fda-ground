package com.boeing.cas.supa.ground.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.boeing.cas.supa.ground.helpers.EmailSender;
import com.boeing.cas.supa.ground.pojos.FileUploadMessage;
import com.boeing.cas.supa.ground.utils.ADWTransferUtil;
import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.boeing.cas.supa.ground.utils.FileUtil;
import com.boeing.cas.supa.ground.utils.ZipFilteredReader;

@RestController
@RequestMapping("/uploadFile")
public class FileUploadController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private static final String SUCCESS_MESSAGE = "Success";
	private static final String FAILURE_MESSAGE = "Fail";
	private static String uploadFolder;

	@RequestMapping(method = {RequestMethod.POST })
	public ResponseEntity<Object> uploadFile(
			@RequestParam("file") MultipartFile uploadfile, HttpServletRequest httpRequest) {


		logger.info("Upload File method invoked");

		logger.debug("Single file upload!");
		if (uploadfile.isEmpty()) {
			//IF FILE IS EMPTY
			FileUploadMessage fum = new FileUploadMessage("Fail", "Fail", "Fail", "Empty File");
			return new ResponseEntity<>(fum, HttpStatus.BAD_REQUEST);
		}
		try {
			saveUploadedFiles(Arrays.asList(uploadfile));
		} catch (IOException e) {
			FileUploadMessage fum = new FileUploadMessage("Fail", "Fail", "Fail", "IO EXCEPTION:" + e.getMessage());
			return new ResponseEntity<>(fum, HttpStatus.BAD_REQUEST);
		}


		Path uploadFolderPath = Paths.get(uploadFolder + File.separator + uploadfile.getOriginalFilename());
		
		//Adding file to ADW
		logger.debug("Adding file to ADW");
		ExecutorService es = Executors.newFixedThreadPool(3);
		List<Future<Boolean>> futures = new ArrayList<>();
		Future<Boolean> adwFuture = es.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				Boolean xfr = false;
				try {
					logger.info("Starting ADW Transfer");
					ADWTransferUtil adw = new ADWTransferUtil();
					logger.info(uploadFolderPath.toFile().getAbsolutePath());
					xfr = adw.sendFile(uploadFolderPath.toFile().getAbsolutePath());
					logger.info("Transfer to ADW complete: " + xfr);
				}
				catch(Exception e) {
					logger.error("Error in ADW XFER: "+e);
				}
				return xfr;
			} 
		});
		
		//Adding file to Azure Storage and Message Queue
		logger.debug("Adding file to Azure Storage and Message Queue");
		Future<Boolean> azureFuture = es.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				Boolean upload = false;
				try {
					logger.info("Starting Azure upload");
					AzureStorageUtil asu = new AzureStorageUtil();
					upload = asu.uploadFile(uploadFolderPath.toFile().getAbsolutePath(), uploadfile.getOriginalFilename());
					logger.info("Upload to Azure complete: " + upload);
				}
				catch(Exception e) {
					logger.error("Error in Azure upload: "+e);
				}
				return upload;
			} 
		});
		
		
		//Sending email to logged in user
		logger.debug("Sending email to logged in user...");
		Future<Boolean> emailFuture = es.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				Boolean sent = false;
				try {
					Path tempDirPath = Files.createTempDirectory("");
					if(uploadfile.getOriginalFilename().endsWith("zip")){
						logger.debug("It is zip file");
						ZipFilteredReader zipFilteredReader = new ZipFilteredReader(uploadFolderPath.toFile().getAbsolutePath(), tempDirPath.toString());
						zipFilteredReader.filteredExpandZipFile(zipEntry -> {
							String entryLC = zipEntry.getName().toLowerCase();
							return !zipEntry.isDirectory() && entryLC.indexOf('/') == -1
									&& (entryLC.endsWith(".csv") || entryLC.endsWith(".dat") || entryLC.endsWith(".sav")
											|| entryLC.endsWith(".info") || entryLC.endsWith(".json") || entryLC.endsWith(".properties")
											|| entryLC.endsWith(".txt"));
						});
					}
					logger.info("Sending Email");
					Optional<File> flightProgress = FileUtil.getFileByNameFromDirectory(tempDirPath, "flight_progress.csv");
					EmailSender ems = new EmailSender();
					sent = ems.sendEmail(Arrays.asList(flightProgress.isPresent() ? flightProgress.get() : new File("")), "mihir.shah@boeing.com",  "Test Flight Progress File");
					logger.info("Email sent: " + sent);
				}
				catch(Exception e) {
					logger.error("Error in email sending: "+e);
				}
				return sent;
			} 
		});
		
		futures.add(emailFuture);
		futures.add(adwFuture);
		futures.add(azureFuture);
		boolean azureBool = false;
		boolean adwBool = false;
		boolean emailBool = false;
		try {
			azureBool = azureFuture.get();
			adwBool = adwFuture.get();
			emailBool = emailFuture.get();
		} catch (InterruptedException | ExecutionException e) {
			logger.error("Error in running executionservice: "+e.getMessage());
		}
		es.shutdown();
		try {
			if (!es.awaitTermination(1, TimeUnit.MINUTES)) {
				es.shutdownNow();
			} 
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.error("Error in shuttingdown executionservice: "+e.getMessage());
			es.shutdownNow();
		}
		FileUploadMessage fum = new FileUploadMessage(adwBool ? SUCCESS_MESSAGE : FAILURE_MESSAGE , azureBool ? SUCCESS_MESSAGE : FAILURE_MESSAGE, emailBool ? SUCCESS_MESSAGE : FAILURE_MESSAGE, "Uploaded File: " +uploadfile.getOriginalFilename());
		return new ResponseEntity<>(fum, HttpStatus.OK);
	}

	private static void saveUploadedFiles(List<MultipartFile> files) throws IOException {		
		for (MultipartFile file : files) {
			if (file.isEmpty()) {
				continue; //next pls
			}

			byte[] bytes = file.getBytes();
			
			Path tempDirPath = Files.createTempDirectory("");
			uploadFolder = tempDirPath.toString();
			Path path = Paths.get(uploadFolder + File.separator + file.getOriginalFilename());
			Files.write(path, bytes);
		}

	}
}
