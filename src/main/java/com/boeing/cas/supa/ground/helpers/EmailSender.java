package com.boeing.cas.supa.ground.helpers;

import java.util.Properties;

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

public class EmailSender {
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
	public static void SendMail() throws Exception {
		Properties mailServerProperties = System.getProperties();
		mailServerProperties.put("mail.transport.protocol", "smtp");
		mailServerProperties.put("mail.smtp.host", SMTP_HOST_NAME);
		mailServerProperties.put("mail.smtp.port", 587);
		mailServerProperties.put("mail.smtp.auth", "true");
		Authenticator auth = new SMTPAuthenticator();
		Session mailSession = Session.getInstance(mailServerProperties, auth);
		MimeMessage message = new MimeMessage(mailSession);
		Multipart multipart = new MimeMultipart("alternative");
		BodyPart part1 = new MimeBodyPart();
		part1.setText("Hello, Your Contoso order has shipped. Thank you, John");
		BodyPart part2 = new MimeBodyPart();
		String emailBody =
                "\nThe File was uploaded from FDA application. " +
                "\n\n\nPlease reply to rickblair@mac.com to acknowledge" +
                "\nRegards, " +
                "\nFDA Admin\n\n\n\n";
		part2.setContent(emailBody, "text/html");
		multipart.addBodyPart(part1);
		multipart.addBodyPart(part2);
		message.setFrom(new InternetAddress("john@contoso.com"));
		message.addRecipient(Message.RecipientType.TO,
		   new InternetAddress("mihir.shah@boeing.com"));
		message.setSubject("Test Flight Progress File");
		message.setContent(multipart);
		
		Transport transport = mailSession.getTransport();
		// Connect the transport object.
		transport.connect();
		// Send the message.
		transport.sendMessage(message, message.getAllRecipients());
		// Close the connection.
		transport.close();
	}
}
