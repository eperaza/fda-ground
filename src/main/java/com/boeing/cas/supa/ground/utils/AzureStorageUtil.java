package com.boeing.cas.supa.ground.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;

import com.boeing.cas.supa.ground.exceptions.SupaSystemLogException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boeing.cas.supa.ground.exceptions.FlightRecordException;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.AzureStorageMessage;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.pojos.UserCondensed;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.storage.AccessCondition;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobRequestOptions;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;

public class AzureStorageUtil {

	private final Logger logger = LoggerFactory.getLogger(AzureStorageUtil.class);
	
	private String accountName;
	private String key;
	private CloudStorageAccount storageAccount;

	public AzureStorageUtil(String accountName, String key) throws IOException {

		try {
			this.accountName = accountName;
			this.key = key;
			this.storageAccount = this.getcloudStorageAccount();
		} catch (IllegalArgumentException e) {
            this.logger.error("IllegalArgumentException: {}", e.getMessage(), e);
        } catch (RuntimeException e) {
            this.logger.error("RuntimeException: {}", e.getMessage(), e);
        } catch (Exception e) {
            this.logger.error("Exception: {}", e.getMessage(), e);
        }
	}

	public boolean uploadFile(String fileLocation, String fileName, User user) {

		String containerName = fileName.split("_")[0].toLowerCase();
		boolean rval = false;

		try {

			CloudBlobClient serviceClient = this.storageAccount.createCloudBlobClient();
            // NOTE: Container name must be lower case.
            CloudBlobContainer container = serviceClient.getContainerReference(containerName);
            container.createIfNotExists();
            // Upload the file as a Blob.
            CloudBlockBlob blob = container.getBlockBlobReference(fileName);
            // NOTE: If the Blob exists currently we are overriding the existing blob.
			// possibly never be an issue since most file names are uniquely identifiable.
            File sourceFile = new File(fileLocation);
            try (InputStream sourceStream = new FileInputStream(sourceFile)) {
                blob.upload(sourceStream, sourceFile.length());
            }
            // Once the Blob is uploaded, add message to an Azure storage queue.
            CloudQueueClient queueClient = this.storageAccount.createCloudQueueClient();
            CloudQueue queue = queueClient.getQueueReference("csvqueue");
            AzureStorageMessage msg = new AzureStorageMessage();
            msg.setContainerName(containerName);
            msg.setFileName(fileName);
            msg.setUploadedBy(new UserCondensed(user.getSurname(), user.getGivenName(), user.getDisplayName(), user.getOtherMails(), user.getGroups()));
            msg.setUploadedOn(System.currentTimeMillis() / 1_000L);
            ObjectMapper mapper = new ObjectMapper();
            String jsonMessage = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(msg);
            // Create the queue if it does not exist.
            queue.createIfNotExists();
            CloudQueueMessage message = new CloudQueueMessage(jsonMessage);
            queue.addMessage(message);
            rval = true;
        }
        catch (FileNotFoundException fnfe) {
        	logger.error("FileNotFoundException encountered: {}", fnfe.getMessage(), fnfe);
        }
        catch (StorageException se) {
        	logger.error("StorageException encountered: {}", se.getMessage(), se);
        }
        catch (Exception e) {
        	logger.error("Exception encountered: {}", e.getMessage(), e);
        }

		return rval;
	}

    public boolean uploadFile(String fileLocation, String fileName) {

        String containerName = "tmp";
        boolean rval = false;

        try {

            CloudBlobClient serviceClient = this.storageAccount.createCloudBlobClient();
            // NOTE: Container name must be lower case.
            CloudBlobContainer container = serviceClient.getContainerReference(containerName);
            container.createIfNotExists();
            // Upload the file as a Blob.
            CloudBlockBlob blob = container.getBlockBlobReference(fileName);
            // NOTE: If the Blob exists currently we are overriding the existing blob.
            // possibly never be an issue since most file names are uniquely identifiable.
            File sourceFile = new File(fileLocation);
            try (InputStream sourceStream = new FileInputStream(sourceFile)) {
                blob.upload(sourceStream, sourceFile.length());
            }
            rval = true;
        }
        catch (FileNotFoundException fnfe) {
            logger.error("FileNotFoundException encountered: {}", fnfe.getMessage(), fnfe);
        }
        catch (StorageException se) {
            logger.error("StorageException encountered: {}", se.getMessage(), se);
        }
        catch (Exception e) {
            logger.error("Exception encountered: {}", e.getMessage(), e);
        }

        return rval;
    }


    public boolean uploadFlightRecord(String containerName, String fileName, String sourceFilePath, User user) throws FlightRecordException {

		boolean rval = false;

		try {

			CloudBlobClient serviceClient = this.storageAccount.createCloudBlobClient();
            // NOTE: Container name must be lower case.
            CloudBlobContainer container = serviceClient.getContainerReference(containerName);
            container.createIfNotExists();
            // Upload the file as a Blob.
            CloudBlockBlob blob = container.getBlockBlobReference(fileName);
            // NOTE: If the Blob exists currently we are overriding the existing blob.
			// possibly never be an issue since most file names are uniquely identifiable.
            File sourceFile = new File(sourceFilePath);
            try (InputStream sourceStream = new FileInputStream(sourceFile)) {
                AccessCondition accessCondition = AccessCondition.generateIfNotExistsCondition();
                BlobRequestOptions options = null;
                OperationContext context = new OperationContext();
                blob.upload(sourceStream, sourceFile.length(), accessCondition, options, context);
            }
            // Once the Blob is uploaded, add message to an Azure storage queue.
            CloudQueueClient queueClient = this.storageAccount.createCloudQueueClient();
            CloudQueue queue = queueClient.getQueueReference("csvqueue");
            AzureStorageMessage msg = new AzureStorageMessage();
            msg.setContainerName(containerName);
            msg.setFileName(fileName);
            msg.setUploadedBy(new UserCondensed(user.getSurname(), user.getGivenName(), user.getDisplayName(), user.getOtherMails(), user.getGroups()));
            msg.setUploadedOn(System.currentTimeMillis() / 1_000L);
            ObjectMapper mapper = new ObjectMapper();
            String jsonMessage = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(msg);
            // Create the queue if it does not exist.
            queue.createIfNotExists();
            CloudQueueMessage message = new CloudQueueMessage(jsonMessage);
            queue.addMessage(message);
            rval = true;
        }
        catch (FileNotFoundException fnfe) {
        	logger.error("FileNotFoundException encountered: {}", fnfe.getMessage(), fnfe);
			throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPLOAD_FAILURE", fnfe.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
        }
        catch (StorageException se) {
        	logger.error("StorageException encountered: {}", se.getMessage(), se);
			throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPLOAD_FAILURE", se.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
        }
        catch (Exception e) {
        	logger.error("Exception encountered: {}", e.getMessage(), e);
			throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPLOAD_FAILURE", ExceptionUtils.getRootCause(e).getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
        }

		return rval;
	}


    public boolean BlobExistsOnCloud(String containerName, String fileName) throws URISyntaxException, StorageException
    {
        CloudBlobClient serviceClient = storageAccount.createCloudBlobClient();

        return serviceClient.getContainerReference(containerName)
                .getBlockBlobReference(fileName)
                .exists();
    }


    public boolean uploadSupaSystemLog(String containerName, String fileName, String sourceFilePath) throws SupaSystemLogException {

        boolean rval = false;

        try {

            CloudBlobClient serviceClient = this.storageAccount.createCloudBlobClient();
            // NOTE: Container name must be lower case.
            CloudBlobContainer container = serviceClient.getContainerReference(containerName);
            container.createIfNotExists();
            // Upload the file as a Blob.
            CloudBlockBlob blob = container.getBlockBlobReference(fileName);
            // NOTE: If the Blob exists currently we are overriding the existing blob.
            // possibly never be an issue since most file names are uniquely identifiable.
            File sourceFile = new File(sourceFilePath);
            try (InputStream sourceStream = new FileInputStream(sourceFile)) {
                AccessCondition accessCondition = AccessCondition.generateIfNotExistsCondition();
                BlobRequestOptions options = null;
                OperationContext context = new OperationContext();
                blob.upload(sourceStream, sourceFile.length(), accessCondition, options, context);
            }
            rval = true;
        }
        catch (FileNotFoundException fnfe) {
            logger.error("FileNotFoundException encountered: {}", fnfe.getMessage(), fnfe);
            throw new SupaSystemLogException(new ApiError("SUPA_SYSTEM_LOG_UPLOAD_FAILURE", fnfe.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
        }
        catch (StorageException se) {
            logger.error("StorageException encountered: {}", se.getMessage(), se);
            throw new SupaSystemLogException(new ApiError("SUPA_SYSTEM_LOG_UPLOAD_FAILURE", se.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
        }
        catch (Exception e) {
            logger.error("Exception encountered: {}", e.getMessage(), e);
            throw new SupaSystemLogException(new ApiError("SUPA_SYSTEM_LOG_UPLOAD_FAILURE", ExceptionUtils.getRootCause(e).getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
        }

        return rval;
    }

    public File downloadBlobReferencedInMessage(String containerName, String fileName, String airline)
            throws IOException, URISyntaxException, StorageException {
        // need to get just the file name without path.
        //String justFileName = FilenameUtils.getName(fileName);
        Path path = Files.createTempDirectory(StringUtils.EMPTY);
        File tempFile = new File(path.toString() + File.separator + fileName);

        // Create the Azure Storage Blob Client.
        CloudBlobClient blobClient = this.storageAccount.createCloudBlobClient();

        if (airline != null) {
            fileName = airline + File.separator + fileName;
        }

        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        CloudBlockBlob blob = container.getBlockBlobReference(fileName);
        blob.downloadToFile(tempFile.getAbsolutePath());
        return tempFile;
    }


    public ByteArrayOutputStream downloadFile(String containerName, String fileName) {

		// Create the Azure Storage Blob Client.
	    CloudBlobClient blobClient = this.storageAccount.createCloudBlobClient();
	    ByteArrayOutputStream outputStream = null; 
	    try {

	    	CloudBlobContainer container = blobClient.getContainerReference(containerName);
            
            if (container.exists()) {
            	
            	CloudBlockBlob blob = container.getBlockBlobReference(fileName);
            	if (blob.exists()) {
                    outputStream = new ByteArrayOutputStream();
                    blob.download((OutputStream) outputStream);
            	}
            }            
		} catch (StorageException | URISyntaxException e) {
            logger.error("Failed to download file: {}", e.getMessage(), e);
		}

	    return outputStream;
	}
	
	private CloudStorageAccount getcloudStorageAccount() {

        try {
        	String storageConnectionString = new StringBuilder()
        			.append("DefaultEndpointsProtocol=https;AccountName=").append(this.accountName)
        			.append(";AccountKey=").append(this.key)
        			.append(";EndpointSuffix=core.windows.net;")
        			.toString();
            return CloudStorageAccount.parse(storageConnectionString);
        }
        catch (IllegalArgumentException e) {
        	logger.error("Connection string contains missing or invalid argument(s).");
        	logger.error("Please confirm the connection string is in the Azure connection string format.");
            throw new IllegalArgumentException();
        }
        catch (URISyntaxException urise){
        	logger.error("Connection string specifies an invalid URI.");
        	logger.error("Please confirm the connection string is in the Azure connection string format.");
            throw new IllegalArgumentException();
        }
        catch (InvalidKeyException e) {
        	logger.error("Connection string specifies an invalid key.");
        	logger.error("Please confirm the AccountName and AccountKey in the connection string are valid.");
        	throw new IllegalArgumentException();
        }
    }
}
