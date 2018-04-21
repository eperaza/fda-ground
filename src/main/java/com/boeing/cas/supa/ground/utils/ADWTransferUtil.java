package com.boeing.cas.supa.ground.utils;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;

public class ADWTransferUtil {

	private final Logger logger = LoggerFactory.getLogger(ADWTransferUtil.class);

	private String host;
	private String usr;
	private String pwd;
	private String path;

	public ADWTransferUtil(String adwHost, String adwUser, String adwPwd, String adwPath) {

		this.host = adwHost;
		this.usr = adwUser;
		this.pwd = adwPwd;
		this.path = adwPath;
	}
	
	public boolean sendFile(String fileName) throws IOException {

		boolean rval = false;
		if (host != null) {

			SSHClient ssh = new SSHClient();
			try {

				ssh.addHostKeyVerifier("39:45:c9:85:ff:8f:f5:d9:a3:a0:23:ed:02:f8:cc:b9");
				ssh.connect(host);
				ssh.authPassword(usr, pwd);
				try (SFTPClient sftp = ssh.newSFTPClient()) {

					logger.info("ADWTransfer: PUT");
					sftp.put(fileName, path);
					rval = true;
					logger.info("ADWTransfer: PUT DONE");
				}
			} finally {
				try { ssh.close(); ssh.disconnect(); } catch (Exception e) { /* do nothing */ }
			}
		}

		return rval;
	}
}
