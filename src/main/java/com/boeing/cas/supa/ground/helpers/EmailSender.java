package com.boeing.cas.supa.ground.helpers;

import java.io.File;
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
	private final static Logger logger = LoggerFactory.getLogger(EmailSender.class);
	private static final String SMTP_HOST_NAME = "smtp.sendgrid.net";
	private static final String SMTP_AUTH_USER = "azure_98179abdc222df2e24ff62adcbedd1c0@azure.com";
	private static final String SMTP_AUTH_PWD = "GB7US7aQ4bynfD4h65";
	private static class SMTPAuthenticator extends javax.mail.Authenticator {
		public PasswordAuthentication getPasswordAuthentication() {
			String username = SMTP_AUTH_USER;
			String password = SMTP_AUTH_PWD;
			return new PasswordAuthentication(username, password);
		}
	}
	public static boolean sendEmail(List<File> fileNames, String to, String subject){
		boolean rval = false;
		try{
			Properties mailServerProperties = System.getProperties();
			mailServerProperties.put("mail.transport.protocol", "smtp");
			mailServerProperties.put("mail.smtp.host", SMTP_HOST_NAME);
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
				String file = p.getFileName().toString();
				logger.info("Adding "+file+" to email");
				DataSource source = new FileDataSource(fileName);
				attachmentPart.setDataHandler(new DataHandler(source));
				// This example uses the local file name as the attachment name.
				// They could be different if you prefer.
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
			System.err.println("Mail Sent: "+subject);
		}catch (Exception ex){
			System.err.println("Mail Sending Error! "+ex);
			ex.printStackTrace();
			rval = false;
		}
		return rval;
	}
}
