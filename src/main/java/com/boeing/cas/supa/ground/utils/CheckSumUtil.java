package com.boeing.cas.supa.ground.utils;


import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipInputStream;

public class CheckSumUtil {

//    public static String getSHA256(ZipOutputStream zipfile) throws NoSuchAlgorithmException, IOException {
//        try (ZipOutputStream is = new ZipOutputStream(zipfile)) {
//            return getChecksum(is, "SHA-256");
//        }
//    }
    public static String getSHA256(InputStream file) throws NoSuchAlgorithmException, IOException {
        try (ZipInputStream is = new ZipInputStream(file)) {
            return getChecksum(is, "SHA-256");
        }
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