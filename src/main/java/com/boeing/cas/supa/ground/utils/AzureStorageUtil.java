package com.boeing.cas.supa.ground.utils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private String key;
	private CloudStorageAccount strAccount;

	
	public AzureStorageUtil(String key) throws IOException {
		try {
			this.key = key;
			this.strAccount = getcloudStorageAccount();
		} catch (IllegalArgumentException e) {
			logger.error(e.getMessage());
		} catch (RuntimeException e) {
			logger.error(e.getMessage());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}
	public boolean uploadFile(String fileLocation, String fileName, User user){
		String containerName = fileName.split("_")[0].toLowerCase();		
		boolean rval = false;
		
		try {
            CloudBlobClient serviceClient = strAccount.createCloudBlobClient();            
           
            // Container name must be lower case.
            CloudBlobContainer container = serviceClient.getContainerReference(containerName);
            container.createIfNotExists();

            // Upload an zip file.
            CloudBlockBlob blob = container.getBlockBlobReference(fileName);
            
            
            /* if the blob exists currently we are overriding the existing blob
             * possibly never be problem since most file name are uniquely identifiable
             */
            File sourceFile = new File(fileLocation);
            try(InputStream sourceStream = new FileInputStream(sourceFile)){
                blob.upload(sourceStream, sourceFile.length());
            }

            
            //Once the blob is uploaded add message to a storage queue
            CloudQueueClient queueClient = strAccount.createCloudQueueClient();

            // Retrieve a reference to a queue.
            CloudQueue queue = queueClient.getQueueReference("csvqueue");
            AzureStorageMessage msg = new AzureStorageMessage();
            msg.setContainerName(containerName);
            msg.setFileName(fileName);
            msg.setUploadedBy(new UserCondensed(user.getSurname(), user.getGivenName(), user.getDisplayName(), user.getOtherMails(), user.getGroups()));
            msg.setUploadedOn(System.currentTimeMillis()/1000);
            ObjectMapper mapper = new ObjectMapper();
            String jsonMessage = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(msg);
            
            // Create the queue if it doesn't already exist.
            queue.createIfNotExists();
            CloudQueueMessage message = new CloudQueueMessage(jsonMessage);
            queue.addMessage(message);
            
            rval = true;
        }
        catch (FileNotFoundException fileNotFoundException) {
        	logger.error("FileNotFoundException encountered: ");
        	logger.error(fileNotFoundException.getMessage());
            rval = false;
        }
        catch (StorageException storageException) {
        	logger.error("StorageException encountered: ");
        	logger.error(storageException.getMessage());
            rval = false;
        }
        catch (Exception e) {
        	logger.error("Exception encountered: ");
        	logger.error(e.getMessage());
            rval = false;
        }
		return rval;
	}
	
	public ByteArrayOutputStream downloadFile(String containerName, String fileName){
		// Create the blob client.
	    CloudBlobClient blobClient = strAccount.createCloudBlobClient();
	    ByteArrayOutputStream outputStream = null; 
	    try {
			CloudBlobContainer container = blobClient.getContainerReference(containerName);
			if(container.exists()){
				CloudBlockBlob blob = container.getBlockBlobReference(fileName);
				if(blob.exists()){
					outputStream = new ByteArrayOutputStream();  
					blob.download(outputStream);
				}
			}
		} catch (URISyntaxException | StorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return outputStream;
	}
	
	private CloudStorageAccount getcloudStorageAccount() {

        CloudStorageAccount storageAccount;
        try {
        	logger.info("storage key:" + key);
        	String storageConnectionString = "DefaultEndpointsProtocol=http;" + "AccountName=fdaadw;"
        			+ "AccountKey=" + key + ";EndpointSuffix=core.windows.net;";
            storageAccount = CloudStorageAccount.parse(storageConnectionString);
        }
        catch (IllegalArgumentException e) {
        	logger.error("\nConnection string specifies an invalid URI.");
        	logger.error("Please confirm the connection string is in the Azure connection string format.");
            throw new IllegalArgumentException();
        }
        catch(URISyntaxException e){
        	logger.error("\nConnection string specifies an invalid URI.");
        	logger.error("Please confirm the connection string is in the Azure connection string format.");
            throw new IllegalArgumentException();
        }
        catch (InvalidKeyException e) {
        	logger.error("\nConnection string specifies an invalid key.");
        	logger.error("Please confirm the AccountName and AccountKey in the connection string are valid.");
        	throw new IllegalArgumentException();
        }

        return storageAccount;
    }
}
