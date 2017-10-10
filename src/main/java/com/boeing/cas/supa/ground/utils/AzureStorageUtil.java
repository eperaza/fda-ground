package com.boeing.cas.supa.ground.utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Properties;

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
	static String storageKey = "xBAEcHehUPS1S/7vvGCBomoBuN8lzlHQ4h6fTeS2fNciGY68lq82LEmqsGk5GbZd0ZEXZKb6bxAmoOrjop8HVw==";
	public static String storageConnectionString = "DefaultEndpointsProtocol=http;" + "AccountName=fdaadw;"
			+ "AccountKey=" + storageKey + ";EndpointSuffix=core.windows.net;";
	private CloudStorageAccount strAccount;
	public AzureStorageUtil() {
		try {
			this.strAccount = getcloudStorageAccount();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public boolean uploadFile(String fileLocation, String fileName){
		System.out.println("Starting upload now...");
		System.setProperty("http.proxyHost", "www-only-ewa-proxy.web.boeing.com");
		System.setProperty("http.proxyPort", "31061");
		System.setProperty("https.proxyHost", "www-only-ewa-proxy.web.boeing.com");
		System.setProperty("https.proxyPort", "31061");
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
            System.out.println(sourceFile.toString());
            blob.upload(new FileInputStream(sourceFile), sourceFile.length());
            
            
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
            System.out.print("FileNotFoundException encountered: ");
            System.out.println(fileNotFoundException.getMessage());
            rval = false;
        }
        catch (StorageException storageException) {
            System.out.print("StorageException encountered: ");
            System.out.println(storageException.getMessage());
            rval = false;
        }
        catch (Exception e) {
            System.out.print("Exception encountered: ");
            System.out.println(e.getMessage());
            rval = false;
        }
		return rval;
	}
	private static CloudStorageAccount getcloudStorageAccount() throws RuntimeException, IOException, IllegalArgumentException, URISyntaxException, InvalidKeyException {

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
            System.out.println("\nFailed to load StorageAccount.properties file.");
            throw e;
        }

        CloudStorageAccount storageAccount;
        try {
            storageAccount = CloudStorageAccount.parse(storageConnectionString);
        }
        catch (IllegalArgumentException|URISyntaxException e) {
            System.out.println("\nConnection string specifies an invalid URI.");
            System.out.println("Please confirm the connection string is in the Azure connection string format.");
            throw e;
        }
        catch (InvalidKeyException e) {
            System.out.println("\nConnection string specifies an invalid key.");
            System.out.println("Please confirm the AccountName and AccountKey in the connection string are valid.");
            throw e;
        }

        return storageAccount;
    }
}
