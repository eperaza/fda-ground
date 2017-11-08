package com.boeing.cas.supa.ground.helpers;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailHelper {
	EmailHelper() {
    }
	private final static Logger logger = LoggerFactory.getLogger(EmailHelper.class);
	
	public static boolean sendEmail(List<File> fileNames, String to, String subject)
	   {
		   Properties	mailServerProperties = System.getProperties();
		   boolean rval = false;
		   
			mailServerProperties.put("mail.smtp.port", "587");
			mailServerProperties.put("mail.smtp.auth", "true");
			mailServerProperties.put("mail.smtp.starttls.enable", "true");
			
			Session  getMailSession;
			MimeMessage generateMailMessage;
			getMailSession = Session.getDefaultInstance(mailServerProperties, null);
			generateMailMessage = new MimeMessage(getMailSession);
			
			String[] sendTo = to.split(",");
			try
			{
				for (String st : sendTo)
				{
					generateMailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(st.trim()));
				}
				generateMailMessage.setSubject(subject);
				String emailBody = subject+"\nThe File was uploaded from FDA application. \n\n\nRegards, \nFDA Admin\n\n\n\n";
				BodyPart messageBodyPart = new MimeBodyPart();
				messageBodyPart.setText(emailBody);

		        
		         Multipart multipart = new MimeMultipart();
		         // Set text message part
		         multipart.addBodyPart(messageBodyPart);
		         
		         for(File f : fileNames)
		         {
		        	 messageBodyPart = new MimeBodyPart();
		        	 String fileName = f.getAbsolutePath();
		        	 Path p = Paths.get(fileName);
		        	 String file = p.getFileName().toString();
		        	 DataSource source = new FileDataSource(fileName);
		        	 messageBodyPart.setDataHandler(new DataHandler(source));
		        	 messageBodyPart.setFileName(file);
		        	 multipart.addBodyPart(messageBodyPart);
		         }
		         
		        

		         // Send the complete message parts
		         generateMailMessage.setContent(multipart); 
		         Transport transport = getMailSession.getTransport("smtp");

		 		//We need to get this from a properties file!
		 		transport.connect("smtp.gmail.com", "fdawebby@gmail.com", "wiirocG2");
		 		transport.sendMessage(generateMailMessage, generateMailMessage.getAllRecipients());
		 		transport.close();
		 		rval = true;
				logger.info("Mail Sent: "+subject);
			} 
			catch (MessagingException e) 
			{
				logger.error("Mail Sending Error! "+e);
				e.printStackTrace();
				rval = false;
			}
			return rval;
		}

}
