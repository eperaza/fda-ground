/* *****************************************************************************************************************************
 *  
 * BOEING PROPRIETARY, CONFIDENTIAL AND/OR TRADE SECRET    
 * Copyright (c) 2018 Boeing.                      
 * Unpublished Work. All rights reserved.                  
 * GTC (Export) : 5D992.c          
 * EAR No License Required (NLR) Export Handling Note:
 *   No license is required for the dissemination of the commercial information contained herein to foreign persons other
 *   than those from or in the terrorist supporting countries identified in the United States Export Administration Regulations
 *   (EAR) (15 CFR 730-774). It is the responsibility of the individual in control of this data to abide by U.S. export laws.
 *
 * *****************************************************************************************************************************/

package com.boeing.cas.supa.util;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class EncryptionUtils {

    public static String encrypt(byte[] data, char[] key, byte[] salt,
            byte[] iv, short iterations) throws GeneralSecurityException {
        Cipher pbeCipher = cipher(Cipher.ENCRYPT_MODE, key, salt, iv,
                iterations);
        String encrypted = java.util.Base64.getEncoder().encodeToString(
                pbeCipher.doFinal(data));
        zero(data);
        data = null;
        return encrypted;
    }

    public static String encrypt(char[] property, char[] key, byte[] salt,
            byte[] iv, short iterations) throws GeneralSecurityException {
        if (iterations < 1000) // security requirement
            throw new IllegalArgumentException(
                    "Invalid Argument Exception: iterations must be >= 1000. Entered: " + iterations);

        byte[] bytes = new byte[property.length];
        for (int i = 0; i < property.length; i++) {
            bytes[i] = (byte) property[i];
        }
        String encrypted = encrypt(bytes, key, salt, iv, iterations);
        return encrypted;
    }

    public static byte[] decrypt(byte[] bytes, char[] key, byte[] salt,
            byte[] iv, short iterations) throws GeneralSecurityException,
            IOException {
        if (iterations < 1000) // security requirement
            throw new IllegalArgumentException(
                    "Invalid Argument Exception: iterations must be >= 1000. Entered: " + iterations);
        
        byte[] decoded = java.util.Base64.getDecoder().decode(bytes);
        return cipher(Cipher.DECRYPT_MODE, key, salt, iv, iterations).doFinal(
                decoded);
    }

    private static void zero(byte[] arr) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = 0;
        }
    }

    public static Cipher cipher(int mode, char[] key, byte[] salt, byte[] iv,
            short iterations) throws NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException {
        if (iterations < 1000) // security requirement
            throw new IllegalArgumentException(
                    "Invalid Argument Exception: iterations must be >= 1000. Entered: " + iterations);
        
        SecretKeyFactory keyFactory = SecretKeyFactory
                .getInstance("PBEWithHmacSHA256AndAES_128");
        SecretKey secretKey = keyFactory.generateSecret(new PBEKeySpec(key));
        Cipher cipher = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(mode, secretKey, new PBEParameterSpec(salt, iterations,
                ivSpec));
        return cipher;
    }

}