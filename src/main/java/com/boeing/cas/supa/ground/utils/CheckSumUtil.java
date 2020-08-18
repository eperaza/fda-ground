package com.boeing.cas.supa.ground.utils;


import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipInputStream;

public class CheckSumUtil {

    public static String getSHA256(InputStream file) throws NoSuchAlgorithmException, IOException {
        try (ZipInputStream is = new ZipInputStream(file)) {
            return getChecksum(is, "SHA-256");
        }
    }

    public String generateCheckSum(byte[] zipFile) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] utf8 = md.digest(zipFile);
        String checkSum = DatatypeConverter.printHexBinary(utf8);
        return checkSum;
    }

    public static String getChecksum(InputStream is, String hashAlgorithm) throws NoSuchAlgorithmException, IOException {
        MessageDigest algorithm = MessageDigest.getInstance(hashAlgorithm);

        byte[] buffer = new byte[1024];
        int numberOfBytes;
        while ((numberOfBytes = is.read(buffer)) != -1) {
            algorithm.update(buffer, 0, numberOfBytes);
        }

        // bytes to hex
        StringBuilder result = new StringBuilder();
        for (byte b : algorithm.digest()) {
            result.append(String.format("%02x", b));
        }

        return result.toString();
    }
}
