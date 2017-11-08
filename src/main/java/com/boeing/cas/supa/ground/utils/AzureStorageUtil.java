package com.boeing.cas.supa.ground.utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private CloudStorageAccount strAccount;
	public AzureStorageUtil() {
		try {
			this.strAccount = getcloudStorageAccount();
		} catch (InvalidKeyException e) {
			logger.error(e.getMessage());
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		}
	}
	public boolean uploadFile(String fileLocation, String fileName){
		String containerName = fileName.split("_")[0].toLowerCase();		
		boolean rval = false;
		InputStream sourceStream = null;
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
            sourceStream = new FileInputStream(sourceFile);
            blob.upload(sourceStream, sourceFile.length());
            
            //Once the blob is uploaded add message to a storage queue
            CloudQueueClient queueClient = strAccount.createCloudQueueClient();

            // Retrieve a reference to a queue.
            CloudQueue queue = queueClient.getQueueReference("csvqueue");
            AzureStorageMessage msg = new AzureStorageMessage();
            msg.setContainerName(containerName);
            msg.setFileName(fileName);
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
        }finally{
        	try {
        		if(sourceStream != null){
        			sourceStream.close();
        		}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.error(e.getMessage());
			}
        }
		return rval;
	}
	private CloudStorageAccount getcloudStorageAccount() throws RuntimeException, IOException, IllegalArgumentException, URISyntaxException, InvalidKeyException {

        // Retrieve the connection string
        Properties prop = new Properties();
        try {
            InputStream propertyStream = AzureStorageUtil.class.getClassLoader().getResourceAsStream("StorageAccount.properties");
            if (propertyStream != null) {
                prop.load(propertyStream);
            }
            else {
                throw new RuntimeException();
            }
        } catch (RuntimeException|IOException e) {
        	logger.error("\nFailed to load StorageAccount.properties file.");
            throw e;
        }
        CloudStorageAccount storageAccount;
        try {
        	String key = prop.getProperty("StorageKey");
        	String storageConnectionString = "DefaultEndpointsProtocol=http;" + "AccountName=fdaadw;"
        			+ "AccountKey=" + key + ";EndpointSuffix=core.windows.net;";
            storageAccount = CloudStorageAccount.parse(storageConnectionString);
        }
        catch (IllegalArgumentException|URISyntaxException e) {
        	logger.error("\nConnection string specifies an invalid URI.");
        	logger.error("Please confirm the connection string is in the Azure connection string format.");
            throw e;
        }
        catch (InvalidKeyException e) {
        	logger.error("\nConnection string specifies an invalid key.");
        	logger.error("Please confirm the AccountName and AccountKey in the connection string are valid.");
            throw e;
        }

        return storageAccount;
    }
}
