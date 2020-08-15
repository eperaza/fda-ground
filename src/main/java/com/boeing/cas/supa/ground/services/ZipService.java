package com.boeing.cas.supa.ground.services;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import org.springframework.stereotype.Service;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

@Service
public class ZipService {
	public File zipFilesEncrypted(String password, String dest, String folder, File... files) throws IOException {
		return zipFilesEncrypted(password, dest, folder, new ArrayList<>(Arrays.asList(files)));
	}
	
	private File zipFilesEncrypted(String password, String dest, String folder, ArrayList<File> files) throws IOException {
		try {
            ZipFile zipFile = new ZipFile(dest);
            zipFile.addFiles(files, ZIP_PARAMS(password));
            if (folder != null) {
            	zipFile.addFolder(folder, ZIP_PARAMS(password));
            }
            return zipFile.getFile();
        } catch (Exception e) {
            throw new IOException(e);
        }
	}
	
	private ZipParameters ZIP_PARAMS(String pw) {
        return new ZipParameters() {
            {
                setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
                setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
                setEncryptFiles(true);
                setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
                setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
                setPassword(pw);
            }
        };
    }
}
