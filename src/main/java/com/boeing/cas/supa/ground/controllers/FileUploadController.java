package com.boeing.cas.supa.ground.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

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
import com.boeing.cas.supa.ground.utils.ADWTransferUtil;
import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.UserInfo;

@RestController
@RequestMapping("/uploadFile")
public class FileUploadController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	//Save the uploaded file to this folder
	public static final String UPLOADED_FOLDER = System.getProperty("user.dir") + "/Airline_files/";

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
			//IF FILE IS EMPTY...
			return new ResponseEntity<Object>("SUCCESS", HttpStatus.OK);
		}

		try {

			logger.debug("adding the file to local");
			saveUploadedFiles(Arrays.asList(uploadfile));
			logger.debug("Uploaded file");

		} catch (IOException e) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		
		Path path = Paths.get(UPLOADED_FOLDER + uploadfile.getOriginalFilename());
		//Upload file to Azure Storage
		logger.debug("Adding file to Azure Storage and Message Queue");
		
		
		logger.debug("Adding file to ADW");
		Runnable runnable = () -> 
		{
			try {
				AzureStorageUtil asu = new AzureStorageUtil();
				boolean upload = asu.uploadFile(path.toFile().getAbsolutePath(), uploadfile.getOriginalFilename());
				logger.info("Upload complete " + upload);
			}
			catch(Exception e) {

				System.err.println("Error in Azure upload: "+e);
				e.printStackTrace(System.err);
			}
		};
		Thread thread = new Thread(runnable);
		thread.start();
		
		//Upload file to ADW
		logger.debug("Adding file to ADW");
		Runnable runnable1 = () -> 
		{
			try {
				ADWTransferUtil adw = new ADWTransferUtil();
				boolean xfr = adw.sendFile(path.toFile().getAbsolutePath());
				logger.info("Transfer complete " + xfr);
			}
			catch(Exception e) {

				System.err.println("Error in ADW XFER: "+e);
				e.printStackTrace(System.err);
			}
		};
		Thread thread1 = new Thread(runnable1);
		thread1.start();

		return new ResponseEntity<Object>("uploaded - " + uploadfile.getOriginalFilename(), HttpStatus.OK);
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
