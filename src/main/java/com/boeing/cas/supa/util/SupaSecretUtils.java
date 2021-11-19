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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class SupaSecretUtils {
	private static final byte[] SALT = { (byte) 0xfe, (byte) 0xf1, (byte) 0xf0,
            (byte) 0xf0, (byte) 0x11, (byte) 0xde, (byte) 0x1e, (byte) 0xe7, };

    private static final byte[] IV = { (byte) 0xf0, (byte) 0xf0, (byte) 0xf0,
            (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0,
            (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0,
            (byte) 0xf0, (byte) 0xf0, (byte) 0xf0, };

    private static final char[] KEY = { 'Y', 'Y', 'Z', 'V', 'C', 'D', '7', 'v',
            'W', '9', '5', 'S', 'C', 'U', 'Q', 'J' };
    
    private static final short SECURITY_LAYERS = 1004;

    public static String generatePassword(char[] pw, short layers)
            throws GeneralSecurityException, FileNotFoundException, IOException {

        byte[] encryptedValue = EncryptionUtils.encrypt(pw, KEY, SALT, IV,
                layers).getBytes();

        return new String(encryptedValue);
    }
}