package com.boeing.cas.supa.ground.utils;

import com.boeing.cas.supa.ground.exceptions.FlightRecordException;
import com.boeing.cas.supa.ground.exceptions.SupaSystemLogException;
import com.boeing.cas.supa.ground.exceptions.TspConfigLogException;
import com.boeing.cas.supa.ground.pojos.*;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.storage.AccessCondition;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.*;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
        } catch (FileNotFoundException fnfe) {
            logger.error("FileNotFoundException encountered: {}", fnfe.getMessage(), fnfe);
        } catch (StorageException se) {
            logger.error("StorageException encountered: {}", se.getMessage(), se);
        } catch (Exception e) {
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
        } catch (FileNotFoundException fnfe) {
            logger.error("FileNotFoundException encountered: {}", fnfe.getMessage(), fnfe);
        } catch (StorageException se) {
            logger.error("StorageException encountered: {}", se.getMessage(), se);
        } catch (Exception e) {
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
        } catch (FileNotFoundException fnfe) {
            logger.error("FileNotFoundException encountered: {}", fnfe.getMessage(), fnfe);
            throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPLOAD_FAILURE", fnfe.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
        } catch (StorageException se) {
            logger.error("StorageException encountered: {}", se.getMessage(), se);
            throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPLOAD_FAILURE", se.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
        } catch (Exception e) {
            logger.error("Exception encountered: {}", e.getMessage(), e);
            throw new FlightRecordException(new ApiError("FLIGHT_RECORD_UPLOAD_FAILURE", ExceptionUtils.getRootCause(e).getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
        }

        return rval;
    }

    public boolean blobContainerExists(String containerName) {
        try {
            CloudBlobClient serviceClient = storageAccount.createCloudBlobClient();
            return serviceClient.getContainerReference(containerName).exists();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean blobExistsOnCloud(String containerName, String fileName) {
        try {
            CloudBlobClient serviceClient = storageAccount.createCloudBlobClient();

            return serviceClient.getContainerReference(containerName)
                    .getBlockBlobReference(fileName)
                    .exists();
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean archiveFileToSubContainer(String containerName, String airlineGroup ,String fileName) throws TspConfigLogException {
        boolean returnValue = false;

        logger.debug("archiving file ");
        try {
            CloudBlobClient serviceClient = this.storageAccount.createCloudBlobClient();
            // NOTE: Container name must be lower case.
            String airlineGroupContainerName = new StringBuilder(containerName).append("/").append(airlineGroup).toString();
            CloudBlobContainer container = serviceClient.getContainerReference(airlineGroupContainerName);
            if(container == null){
                logger.debug("Container {} does not exist", airlineGroupContainerName);
                return false;
            }

            String archiveContainerName = new StringBuilder(containerName).append("/").append(airlineGroup).append("/archive").toString();
            CloudBlobContainer archiveContainer = serviceClient.getContainerReference(archiveContainerName);
            if(archiveContainer == null){
                logger.debug("Archive container {} does not exist", archiveContainerName);
                return false;
            }

            String currentTime = DateTime.now().toString();
            String archiveBlobName = new StringBuilder(fileName).append("-").append(currentTime).toString();
            CloudBlockBlob archivedBlob = archiveContainer.getBlockBlobReference(archiveBlobName);
            CloudBlockBlob blobToCopy = container.getBlockBlobReference(fileName);
            if(!blobToCopy.exists()){
                logger.debug("Blob to Copy {} does not exist", blobToCopy);
                return false;
            }
            archivedBlob.startCopy(blobToCopy);

            returnValue = true;
        } catch (StorageException se) {
            logger.error("StorageException encountered: {}", se.getMessage(), se);
            throw new TspConfigLogException(new ApiError("TSP_CONFIG_SYSTEM_LOG_FAILURE", se.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
        } catch (URISyntaxException urise) {
            logger.error("URISyntaxException encountered: {}", urise.getMessage(), urise);
            throw new TspConfigLogException(new ApiError("TSP_CONFIG_SYSTEM_LOG_FAILURE", urise.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
        }

        return returnValue;
    }

    public boolean uploadTspZipConfig(String containerName, String fileName, String sourceFilePath) throws TspConfigLogException {
        boolean rval = false;

        logger.debug("got to uploading TSP Zip File! ");
        try {
            CloudBlobClient serviceClient = this.storageAccount.createCloudBlobClient();
            // NOTE: Container name must be lower case.
            CloudBlobContainer container = serviceClient.getContainerReference(containerName);
            container.createIfNotExists();
            // Upload the file as a Blob.
            CloudBlockBlob blob = container.getBlockBlobReference(String.valueOf(fileName));
            // NOTE: If the Blob exists currently we are overriding the existing blob.
            // possibly never be an issue since most file names are uniquely identifiable.
            File sourceFile = new File(sourceFilePath);
            try (InputStream sourceStream = new FileInputStream(sourceFile)) {
                AccessCondition accessCondition = AccessCondition.generateIfExistsCondition();

                BlobRequestOptions options = null;
                OperationContext context = new OperationContext();

                blob.upload(sourceStream, sourceFile.length(), accessCondition, options, context);
            }
            rval = true;
        } catch (FileNotFoundException fnfe) {
            logger.error("FileNotFoundException encountered: {}", fnfe.getMessage(), fnfe);
            throw new TspConfigLogException(new ApiError("TSP_CONFIG_SYSTEM_LOG_FAILURE", fnfe.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
        } catch (StorageException se) {
            logger.error("StorageException encountered: {}", se.getMessage(), se);
            throw new TspConfigLogException(new ApiError("TSP_CONFIG_SYSTEM_LOG_FAILURE", se.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
        } catch (Exception e) {
            logger.error("Exception encountered: {}", e.getMessage(), e);
            throw new TspConfigLogException(new ApiError("TSP_CONFIG_SYSTEM_LOG_FAILURE", ExceptionUtils.getRootCause(e).getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
        }

        return rval;
    }

    public Date getLastModifiedTimeStampFromBlob(String containerName, String fileName) {
        try {
            CloudBlobClient serviceClient = storageAccount.createCloudBlobClient();
            CloudBlobContainer container = serviceClient.getContainerReference(containerName);
            CloudBlockBlob blob = container.getBlockBlobReference(fileName);
            // NOTE: MUST call downloadAttributes or your properties will return null
            blob.downloadAttributes();
            BlobProperties blobProps = blob.getProperties();

            logger.info("Last Modified TimeStamp: " + blobProps.getLastModified());

            Date lastModified = blobProps.getLastModified();

            return lastModified;
        } catch (Exception ex) {
            logger.debug("!! FAILED - could not retrieve LastModifiedTimestamp - " + ex);
        }
        return null;
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
        } catch (FileNotFoundException fnfe) {
            logger.error("FileNotFoundException encountered: {}", fnfe.getMessage(), fnfe);
            throw new SupaSystemLogException(new ApiError("SUPA_SYSTEM_LOG_UPLOAD_FAILURE", fnfe.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
        } catch (StorageException se) {
            logger.error("StorageException encountered: {}", se.getMessage(), se);
            throw new SupaSystemLogException(new ApiError("SUPA_SYSTEM_LOG_UPLOAD_FAILURE", se.getMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
        } catch (Exception e) {
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

    public List<String> getFilenamesFromBlob(String containerName, String airlineGroup) {

        CloudBlobClient blobClient = this.storageAccount.createCloudBlobClient();
        String airlineDir = new StringBuilder(airlineGroup.toUpperCase()).append("/").toString();

        List<String> tspFileNames = new ArrayList<>();

        try {
            CloudBlobContainer container = blobClient.getContainerReference(containerName);

            Iterable<ListBlobItem> blobs = container.listBlobs();

            logger.debug("airlineDir: " + airlineDir);
            int trimIndex = airlineDir.length();

            for (ListBlobItem blob : blobs) {
                CloudBlobDirectory directory = (CloudBlobDirectory) blob;

                if (airlineDir.equals(directory.getPrefix())) {

                    Iterable<ListBlobItem> fileBlobs = directory.listBlobs();
                    for (ListBlobItem fileBlob : fileBlobs) {
                        if (fileBlob instanceof CloudBlob) {
                            CloudBlob cloudBlob = (CloudBlob) fileBlob;
                            // trim the parent directory from the path
                            String tspFileName = new StringBuilder(cloudBlob.getName().substring(trimIndex)).toString();
                            tspFileNames.add(tspFileName);
                        }
                    }
                }
            }
        } catch (StorageException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return tspFileNames;
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
        } catch (IllegalArgumentException e) {
            logger.error("Connection string contains missing or invalid argument(s).");
            logger.error("Please confirm the connection string is in the Azure connection string format.");
            throw new IllegalArgumentException();
        } catch (URISyntaxException urise) {
            logger.error("Connection string specifies an invalid URI.");
            logger.error("Please confirm the connection string is in the Azure connection string format.");
            throw new IllegalArgumentException();
        } catch (InvalidKeyException e) {
            logger.error("Connection string specifies an invalid key.");
            logger.error("Please confirm the AccountName and AccountKey in the connection string are valid.");
            throw new IllegalArgumentException();
        }
    }

    public List<String> getAllBlobNamesInTheContainer(String containerName, String prefix) {
        List<String> blobNames = new ArrayList<String>();
        CloudBlobClient cloudBlobClient = this.storageAccount.createCloudBlobClient();
        try {
            CloudBlobContainer container = cloudBlobClient.getContainerReference(containerName);
            Iterable<ListBlobItem> items;
            if (prefix == null) {
                items = container.listBlobs();
            } else {
                items = container.listBlobs(prefix);
            }

            for (ListBlobItem item : items) {
                if (item instanceof CloudBlobDirectory) {
                    blobNames.addAll(getAllBlobNamesInTheContainer(containerName, ((CloudBlobDirectory) item).getPrefix()));
                } else {
                    String itemUrl = item.getUri().toString();
                    String itemName = itemUrl.substring(itemUrl.lastIndexOf("/") + 1);
                    if (prefix == null) {
                        blobNames.add(itemName);
                    } else {
                        blobNames.add(String.format("%s%s", prefix, itemName));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to list the blobs in a container " + containerName);
        }

        return blobNames;
    }
}
