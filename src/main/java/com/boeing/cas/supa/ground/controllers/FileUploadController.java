package com.boeing.cas.supa.ground.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.boeing.cas.supa.ground.helpers.AzureADAuthHelper;
import com.boeing.cas.supa.ground.helpers.EmailHelper;
import com.boeing.cas.supa.ground.utils.ZipFilteredReader;
import com.boeing.cas.supa.ground.pojos.FileUploadMessage;
import com.boeing.cas.supa.ground.utils.ADWTransferUtil;
import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.boeing.cas.supa.ground.utils.FileUtil;
import com.boeing.cas.supa.ground.utils.MicrosoftGraphUtil;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.UserInfo;

@RestController
@RequestMapping("/uploadFile")
public class FileUploadController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	//Save the uploaded file to this folder
	public static final String UPLOADED_FOLDER = System.getProperty("user.dir") + "/Airline_files/";
	public static final String UNZIPED_FOLDER = System.getProperty("user.dir") + "/Airline_files_unzipped/";

	@RequestMapping(method = {RequestMethod.POST })
	public ResponseEntity<Object> uploadFile(
			@RequestParam("file") MultipartFile uploadfile, HttpServletRequest httpRequest) {


		System.out.println("Upload File method invoked");

		logger.debug("Single file upload!");
		/*HttpSession session = httpRequest.getSession();
        AuthenticationResult result = (AuthenticationResult) session.getAttribute(AzureADAuthHelper.PRINCIPAL_SESSION_NAME);
        if (result == null) {
			//RESPONSE ENTITY NOT AUTHORIZED...
        	return null;
		}else{*/
		//String tenant = session.getServletContext().getInitParameter("tenant");
		//System.out.println("AccessToken: " + result.getAccessToken());
		//System.out.println("Tenant: " + tenant);
		//UserInfo userInfo = result.getUserInfo();
		if (uploadfile.isEmpty()) {
			//IF F EMPTY...ILE IS
			FileUploadMessage fum = new FileUploadMessage("Fail", "Fail", "Fail", "Empty File");
			return new ResponseEntity<Object>(fum, HttpStatus.BAD_REQUEST);
		}

		try {

			logger.debug("adding the file to local");
			saveUploadedFiles(Arrays.asList(uploadfile));
			logger.debug("Uploaded file");

		} catch (IOException e) {
			FileUploadMessage fum = new FileUploadMessage("Fail", "Fail", "Fail", e.getMessage());
			return new ResponseEntity<Object>(fum, HttpStatus.BAD_REQUEST);
		}


		Path path = Paths.get(UPLOADED_FOLDER + uploadfile.getOriginalFilename());
		//Upload file to Azure Storage
		logger.debug("Adding file to ADW");
		ExecutorService es = Executors.newFixedThreadPool(3);
		List<Future<Boolean>> futures = new ArrayList<>();
		Future<Boolean> adwFuture = es.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				Boolean xfr = false;
				try {
					ADWTransferUtil adw = new ADWTransferUtil();
					xfr = adw.sendFile(path.toFile().getAbsolutePath());
					logger.info("Transfer complete " + xfr);
				}
				catch(Exception e) {
					logger.error("Error in ADW XFER: "+e);
				}
				return xfr;
			} 
		});
		logger.debug("Adding file to Azure Storage and Message Queue");
		Future<Boolean> azureFuture = es.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				Boolean upload = false;
				try {
					AzureStorageUtil asu = new AzureStorageUtil();
					upload = asu.uploadFile(path.toFile().getAbsolutePath(), uploadfile.getOriginalFilename());
					logger.info("Upload complete " + upload);
				}
				catch(Exception e) {
					logger.error("Error in Azure upload: "+e);
				}
				return upload;
			} 
		});
		
		
		//Get User's email address and send them an email;
		// TODO Need to get user's alternate email address for now sending it to me....
		logger.debug("Sending email to logged in user");
		Future<Boolean> emailFuture = es.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				Boolean sent = false;
				try {
					Path tempDirPath = Paths.get(UNZIPED_FOLDER + uploadfile.getOriginalFilename());
					 
					if (!Files.exists(tempDirPath)) {
					    try {
					        Files.createDirectory(tempDirPath);
					    } catch (IOException e) {
					        System.err.println(e);
					    }
					}
					if(uploadfile.getOriginalFilename().endsWith("zip")){
						logger.debug("It is zip file");
						ZipFilteredReader zipFilteredReader = new ZipFilteredReader(path.toFile().getAbsolutePath(), tempDirPath.toString());
						zipFilteredReader.filteredExpandZipFile(zipEntry -> {
							String entryLC = zipEntry.getName().toLowerCase();
							return !zipEntry.isDirectory() && entryLC.indexOf('/') == -1
									&& (entryLC.endsWith(".csv") || entryLC.endsWith(".dat") || entryLC.endsWith(".sav")
											|| entryLC.endsWith(".info") || entryLC.endsWith(".json") || entryLC.endsWith(".properties")
											|| entryLC.endsWith(".txt"));
						});
					}
					
					Optional<File> flightProgress = FileUtil.getFileByNameFromDirectory(tempDirPath, "flight_progress.csv");
					sent = EmailHelper.sendEmail(Arrays.asList(flightProgress.isPresent() ? flightProgress.get() : new File("")), "mihir.shah@boeing.com",  "Test Flight Progress File");
					logger.info("Upload complete " + sent);
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
			// TODO Auto-generated catch block
			logger.error("Error in running executionservice: "+e);
		}
		es.shutdown();
		try {
			if (!es.awaitTermination(1, TimeUnit.MINUTES)) {
				es.shutdownNow();
			} 
		} catch (InterruptedException e) {
			logger.error("Error in shuttingdown executionservice: "+e);
			es.shutdownNow();
		}		
		
		FileUploadMessage fum = new FileUploadMessage(adwBool ? "Success" : "Fail" , azureBool ? "Success" : "Fail", emailBool ? "Success" : "Fail", "Uploaded File: " +uploadfile.getOriginalFilename());
		return new ResponseEntity<Object>(fum, HttpStatus.OK);
		//}


	}

	private void saveUploadedFiles(List<MultipartFile> files) throws IOException {

		System.out.println("files => " + files != null ? files.size() : 0);

		for (MultipartFile file : files) {

			System.out.println("file => " + file.getOriginalFilename());
			if (file.isEmpty()) {
				continue; //next pls
			}

			byte[] bytes = file.getBytes();
			Path path = Paths.get(UPLOADED_FOLDER + file.getOriginalFilename());

			Files.write(path, bytes);

		}

	}
}
