package com.boeing.cas.supa.ground.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;

public class ADWTransferUtil {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private String host;
	private String usr;
	private String pwd;
	private String path;
	private SSHClient ssh;

	public ADWTransferUtil() {
		InputStream input = null;
		input = ADWTransferUtil.class.getClassLoader().getResourceAsStream("ADWCredentials.properties");
		if(input == null)
		{
			logger.error("Could not load ADWCredentials.properties file");
			return;
		}
		Properties prop = new Properties();
		try {
			prop.load(input);

			//DEFAULTS NEED TO BE PUNTED
			host = prop.getProperty("adwHost");
			usr = prop.getProperty("adwUser");
			pwd = prop.getProperty("adwPwd");
			path = prop.getProperty("adwPath" );

			logger.info("Properties files loaded successfully: "+ host +":"+path);
		} catch (IOException e) {
			logger.error("Could not load ADWCredentials.properties file: " + e.getMessage());

		}
	}
	
	public boolean sendFile(String fileName) throws IOException {
		boolean rval = false;
		if(host != null)
		{
			ssh = new SSHClient();
			try
			{

				ssh.loadKnownHosts();
				ssh.addHostKeyVerifier("39:45:c9:85:ff:8f:f5:d9:a3:a0:23:ed:02:f8:cc:b9");
				ssh.connect(host);
				ssh.authPassword(usr, pwd);
				SFTPClient sftp = ssh.newSFTPClient();
				try
				{
					logger.info("ADWTransfer: PUT");
					sftp.put(fileName, path);
					rval = true;
					logger.info("ADWTransfer: PUT DONE");
				}
				finally
				{
					sftp.close();
				}
			}
			finally
			{
				ssh.disconnect();
			}
		}
		return rval;
	}
}
