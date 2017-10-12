package com.boeing.cas.supa.ground.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;

public class ADWTransferUtil {
	private String host;
	private String usr;
	private String pwd;
	private String path;
	private static Logger LOGGER = Logger.getLogger(ADWTransferUtil.class.getName());
	public ADWTransferUtil() {
//		System.setProperty("http.proxyHost", "www-only-ewa-proxy.web.boeing.com");
//		System.setProperty("http.proxyPort", "31061");
//		System.setProperty("https.proxyHost", "www-only-ewa-proxy.web.boeing.com");
//		System.setProperty("https.proxyPort", "31061");
		InputStream input = null;
		input = ADWTransferUtil.class.getClassLoader().getResourceAsStream("ADWCredentials.properties");
		if(input == null)
		{
			//Ya I know bad form.  Being Born should not be excuse to die.
			LOGGER.warning("COULD NOT LOAD PROPS FILE");
			//DEBUG ONLY
			
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
		
			LOGGER.info("PROPS LOADED: "+ host +":"+path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOGGER.warning("COULD NOT LOAD PROPS FILE");
			
		}
	}
	public boolean sendFile(String fileName) throws IOException {
		boolean rval = false;
		LOGGER.info("Sending to ADW: "+fileName);
		if(host != null)
		{
			SSHClient ssh = new SSHClient();
			try
			{
				
				//ssh.loadKnownHosts();
				ssh.addHostKeyVerifier("39:45:c9:85:ff:8f:f5:d9:a3:a0:23:ed:02:f8:cc:b9");
				ssh.connect(host);
				ssh.authPassword(usr, pwd);
				SFTPClient sftp = ssh.newSFTPClient();
				try
				{
					System.err.println("ADWTransfer: PUT");
					sftp.put(fileName, path);
					rval = true;
					System.err.println("ADWTransfer: PUT DONE");
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
