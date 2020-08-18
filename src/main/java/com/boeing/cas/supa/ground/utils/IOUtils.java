package com.boeing.cas.supa.ground.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.stream.Stream;

public class IOUtils {
	private final static String SHA256Hash = "SHA-256";
	
	/**
	 * Gets checksum in byte array for a file using SHA-256 hash algorithm.
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static byte[] getChecksumSHA256InByteArray(File file) throws IOException {
		try (FileInputStream is = new FileInputStream(file)) {
			return getCheckSumInByteArray(is, SHA256Hash);
		}
	}
	
	/**
	 * Gets checksum in string for a file using SHA-256 hash algorithm.
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static String getChecksumSHA256InString(File file) throws IOException {
		try (FileInputStream is = new FileInputStream(file)) {
			return getChecksumInString(is, SHA256Hash);
		}
	}
	
	/**
	 * Gets checksum in byte array for an input stream using SHA-256 hash algorithm.
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public static byte[] getChecksumSHA256InByteArray(InputStream is) throws IOException {
		return getCheckSumInByteArray(is, SHA256Hash);
	}
	
	/**
	 * Gets checksum in string for an input stream using SHA-256 hash algorithm.
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public static String getChecksumSHA256InString(InputStream is) throws IOException {
		return getChecksumInString(is, SHA256Hash);
	}
	
	private static byte[] getCheckSumInByteArray(InputStream is, String hashAlgorithm) throws IOException {
		MessageDigest algorithm;
		try {
			algorithm = MessageDigest.getInstance(hashAlgorithm);
		} catch (NoSuchAlgorithmException e) {
			return null;
		}

		byte[] buffer = new byte[8094];
		int numberOfBytes;
		while ((numberOfBytes = is.read(buffer)) != -1) {
			algorithm.update(buffer, 0, numberOfBytes);
		}
		
		return algorithm.digest();
	}

	private static String getChecksumInString(InputStream is, String hashAlgorithm) throws IOException {
		return bytesToHexString(getCheckSumInByteArray(is, hashAlgorithm));
	}
	
	private static String bytesToHexString(byte[] bytes) {
		StringBuilder result = new StringBuilder();
		for (byte b : bytes) {
			result.append(String.format("%02x", b));
		}

		return result.toString();
	}
	
	public static File writeToTempFile(ByteArrayOutputStream outputStream, String filename) throws IOException {
		Path tempDirPath = Files.createTempDirectory(UUID.randomUUID().toString());
		File file = new File(tempDirPath.toString(), filename);
		writeToFile(outputStream.toByteArray(), file, false);
		
		return file;
	}
	
	public static void writeToFile(byte[] bytes, File file, boolean append) throws IOException {
		if (append) {
			Files.write(file.toPath(), bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} else {
			Files.write(file.toPath(), bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		}
	}
	
	public static void deleteDirQuietly(File dir) throws IOException {
        if (!dir.exists()) {
            return;
        }

        try (Stream<Path> files = Files.walk(dir.toPath())) {
            files.map(Path::toFile)
                 .forEach(File::delete);
            
            dir.delete();
        } catch (Exception ex) {}
    }
}
