package com.boeing.cas.supa.ground.helpers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EmailSender {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private String smptHostName;
	private String smptAuthUser;
	private String smptAuthPwd;
	
	public EmailSender() {
		InputStream input = null;
		input = EmailSender.class.getClassLoader().getResourceAsStream("Mail.properties");
		if(input == null)
		{
			logger.error("Could not load Mail.properties file");
			return;
		}
		Properties prop = new Properties();
		try {
			prop.load(input);

			//DEFAULTS NEED TO BE PUNTED
			smptHostName = prop.getProperty("sendgrid.host");
			smptAuthUser = prop.getProperty("sendgrid.user");
			smptAuthPwd = prop.getProperty("sendgrid.password");
			
			logger.info("Properties files loaded successfully: "+ smptHostName +":"+smptAuthUser);
			
		} catch (IOException e) {
			logger.error("Could not load Mail.properties file: " + e.getMessage());

		}
	}
	
	private class SMTPAuthenticator extends javax.mail.Authenticator {
		public PasswordAuthentication getPasswordAuthentication() {
			String username = smptAuthUser;
			String password = smptAuthPwd;
			return new PasswordAuthentication(username, password);
		}
	}
	
	public boolean sendEmail(List<File> fileNames, String to, String subject){
		boolean rval = false;
		try{
			Properties mailServerProperties = System.getProperties();
			mailServerProperties.put("mail.transport.protocol", "smtp");
			mailServerProperties.put("mail.smtp.host", smptHostName);
			mailServerProperties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			mailServerProperties.put("mail.smtp.port", 587);
			mailServerProperties.put("mail.smtp.auth", "true");
			Authenticator auth = new SMTPAuthenticator();
			Session mailSession = Session.getInstance(mailServerProperties, auth);
			MimeMessage message = new MimeMessage(mailSession);
			Multipart multipart = new MimeMultipart("alternative");
			BodyPart bodyText = new MimeBodyPart();
			String emailBody =
					"\nThe File was uploaded from FDA application. " +
							"\n\n\nPlease reply to rickblair@mac.com to acknowledge" +
							"\nRegards, " +
							"\nFDA Admin\n\n\n\n";
			bodyText.setContent(emailBody, "text/html");
			multipart.addBodyPart(bodyText);



			for(File f : fileNames)
			{
				MimeBodyPart attachmentPart = new MimeBodyPart();
				// Specify the local file to attach.
				String fileName = f.getAbsolutePath();
				Path p = Paths.get(fileName);
				DataSource source = new FileDataSource(fileName);
				attachmentPart.setDataHandler(new DataHandler(source));
				attachmentPart.setFileName(p.getFileName().toString());
				multipart.addBodyPart(attachmentPart);
			}
			message.setFrom(new InternetAddress("rickblair@mac.com"));
			String[] sendTo = to.split(",");
			for (String st : sendTo)
			{
				message.addRecipient(Message.RecipientType.TO,
						new InternetAddress(st.trim()));
			}
			
			message.setSubject(subject);
			message.setContent(multipart);

			Transport transport = mailSession.getTransport();
			// Connect the transport object.
			transport.connect();
			// Send the message.
			transport.sendMessage(message, message.getAllRecipients());
			// Close the connection.
			transport.close();
			rval = true;
			logger.info("Mail Sent: "+subject);
		}catch (Exception ex){
			logger.error("Mail Sending Error! "+ex.getMessage());
			rval = false;
		}
		return rval;
	}
	
}
