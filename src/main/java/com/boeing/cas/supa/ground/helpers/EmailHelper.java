package com.boeing.cas.supa.ground.helpers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

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

public class EmailHelper {
	private static Logger logger = Logger.getLogger(EmailHelper.class.getName());
	private static Properties mailConfig = null;
	public static boolean sendEmail(List<File> fileNames, String to, String subject) {
        Properties config = getMailConfig();
        String mailSystem = config.getProperty("mail.system");
        Properties mailServerProperties = System.getProperties();
        boolean rval = false;

        if (!mailSystem.equals("gmail") && !mailSystem.equals("sendgrid")) {
            logger.warning("Email aborted; Invalid email system defined: " + mailSystem);
            return false;
        }

        //apply smtp settings based on configured system
        mailServerProperties.put("mail.transport.protocol", "smtp");
        mailServerProperties.put("mail.smtp.host",
                config.getProperty(mailSystem + ".smtp.host"));
        mailServerProperties.put("mail.smtp.port",
                config.getProperty(mailSystem + ".smtp.port"));
        mailServerProperties.put("mail.smtp.auth",
                config.getProperty(mailSystem + ".smtp.auth"));

        if (mailSystem.equals("gmail")) {
            mailServerProperties.put("mail.smtp.starttls.enable",
                    config.getProperty(mailSystem + ".smtp.starttls.enable"));
        }

        Session getMailSession;
        MimeMessage generateMailMessage;
        getMailSession = Session.getDefaultInstance(mailServerProperties, null);
        generateMailMessage = new MimeMessage(getMailSession);

        String[] sendTo = to.split(",");
        try {
            for (String st : sendTo) {
                generateMailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(st.trim()));
            }
            generateMailMessage.setSubject(subject);
            String emailBody = subject +
                    "\nThe File was uploaded from FDA application. " +
                    "\n\n\nPlease reply to rickblair@mac.com to acknowledge" +
                    "\nRegards, " +
                    "\nFDA Admin\n\n\n\n";
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(emailBody);


            Multipart multipart = new MimeMultipart();
            // Set text message part
            multipart.addBodyPart(messageBodyPart);

            for (File f : fileNames) {
                messageBodyPart = new MimeBodyPart();
                String fileName = f.getAbsolutePath();
                Path p = Paths.get(fileName);
                String file = p.getFileName().toString();
                logger.info("Adding " + file + " to email");
                DataSource source = new FileDataSource(fileName);
                messageBodyPart.setDataHandler(new DataHandler(source));
                messageBodyPart.setFileName(file);
                multipart.addBodyPart(messageBodyPart);
            }


            // Send the complete message parts
            generateMailMessage.setContent(multipart);
            Transport transport = getMailSession.getTransport("smtp");
            transport.connect(
                    config.getProperty(mailSystem + ".host"),
                    config.getProperty(mailSystem + ".user"),
                    config.getProperty(mailSystem + ".password"));
            transport.sendMessage(generateMailMessage, generateMailMessage.getAllRecipients());
            transport.close();
            rval = true;
            System.err.println("Mail Sent: " + subject);
        } catch (MessagingException e) {
            System.err.println("Mail Sending Error! " + e);
            e.printStackTrace();
            rval = false;
        }
        return rval;
    }

    /**
     * Lazy-load mail configuration from mail.properties file in classpath.
     * Defaults to gmail if properties not found/invalid.
     * @return mailConfig properties
     */
    private static Properties getMailConfig() {
        //lazy load mail.properties
        if (mailConfig != null) {
            return mailConfig;
        }

        InputStream input = null;
		input = EmailHelper.class.getClassLoader().getResourceAsStream("Mail.properties");
        if (input != null) {
            mailConfig = new Properties();
            try {
                mailConfig.load(input);
                logger.info("mail.properties loaded");
            } catch (IOException e) {
                e.printStackTrace();
                logger.warning("COULD NOT LOAD mail.properties FILE");
            }
        }

        if (mailConfig == null) {
            //fallback to defaults
            mailConfig = new Properties();
            mailConfig.put("mail.system", "gmail");
            mailConfig.put("gmail.host", "smtp.gmail.com");
            mailConfig.put("gmail.user", "fdawebby@gmail.com");
            mailConfig.put("gmail.password", "wiirocG2");
            mailConfig.put("gmail.smtp.port", "587");
            mailConfig.put("gmail.smtp.auth", "true");
            mailConfig.put("gmail.smtp.starttls.enable", "true");
        }

        return mailConfig;
    }

}
