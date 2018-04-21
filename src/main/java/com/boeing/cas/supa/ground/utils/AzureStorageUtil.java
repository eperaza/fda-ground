package com.boeing.cas.supa.ground.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boeing.cas.supa.ground.pojos.AzureStorageMessage;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.pojos.UserCondensed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;

public class AzureStorageUtil {

	private final Logger logger = LoggerFactory.getLogger(AzureStorageUtil.class);
	
	private String key;
	private CloudStorageAccount storageAccount;

	public AzureStorageUtil(String key) throws IOException {

		try {
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
        	String storageConnectionString = new StringBuilder("DefaultEndpointsProtocol=http;AccountName=").append("fdaadw")
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
