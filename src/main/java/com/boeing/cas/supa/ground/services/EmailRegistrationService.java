package com.boeing.cas.supa.ground.services;

import com.boeing.cas.supa.ground.dao.UserAccountRegistrationDao;
import com.boeing.cas.supa.ground.exceptions.UserAccountRegistrationException;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.utils.ActivationCodeGenerator;
import com.boeing.cas.supa.ground.utils.Constants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmailRegistrationService {

    @Autowired
    private Map<String, String> appProps;

    private final Logger logger = LoggerFactory.getLogger(AzureADClientService.class);

    @Autowired
    private UserAccountRegistrationDao userAccountRegister;

    @Autowired
    private AzureADClientService aadClient;

    public String getNewActivationEmailForUser(User newUser, String registrationToken)
            throws UserAccountRegistrationException {

        String fdadvisorClientCertBase64 = new StringBuilder(appProps.get("FDAdvisorClientCertName")).append("base64").toString();

        String base64EncodedPayload = Base64.getEncoder().encodeToString(
                new StringBuilder(newUser.getUserPrincipalName()).append(' ').append(registrationToken)
                        .append(' ').append(this.appProps.get(fdadvisorClientCertBase64)).toString().getBytes());

        String emailAddress = newUser.getOtherMails().get(0);
        String activationCode = ActivationCodeGenerator.randomString(6);

        StringBuilder emailMessageBody = new StringBuilder();
        String airline = newUser.getGroups().stream()
                .filter(group -> group.getDisplayName().startsWith("airline-"))
                .map(group -> group.getDisplayName().replace("airline-", StringUtils.EMPTY).toUpperCase())
                .collect(Collectors.joining(","));
        emailMessageBody.append("Hi ").append(newUser.getDisplayName()).append(' ')
                .append(String.format("(%s),", airline));

        String role = newUser.getGroups().stream()
                .filter(group -> group.getDisplayName().startsWith("role-"))
                .map(group -> group.getDisplayName().replace("role-airline", StringUtils.EMPTY).toLowerCase())
                .collect(Collectors.joining(","));

        emailMessageBody.append(Constants.HTML_LINE_BREAK).append(Constants.HTML_LINE_BREAK);
        emailMessageBody.append("Your new account for FliteDeck Advisor has been successfully created. The Airline ").append(String.format("%s",
                StringUtils.capitalize(role))).append( " role is assigned to your account.");

        emailMessageBody.append(Constants.HTML_LINE_BREAK).append(Constants.HTML_LINE_BREAK);
        emailMessageBody.append("To get started with your new account registration,");

        emailMessageBody.append(Constants.HTML_LINE_BREAK).append(Constants.HTML_LINE_BREAK);
        emailMessageBody.append("   1. Go to the <a href=\"https://itunes.apple.com/us/app/flitedeck-advisor/id1058617698\">App Store</a>")
                .append(" to install FliteDeck Advisor on your iPad. Open the installed application.").append(Constants.HTML_LINE_BREAK);
        emailMessageBody.append("   2. Come back to this email and enter this <b>ONE-TIME</b> code: \"" + activationCode + "\" into the FliteDeck Registration. ")
                .append(Constants.HTML_LINE_BREAK);
        emailMessageBody.append("   3. After completing the registration and the WiFi configuration, reopen the FliteDeck Advisor to start using it.");

        emailMessageBody.append(Constants.HTML_LINE_BREAK).append(Constants.HTML_LINE_BREAK);
        emailMessageBody.append("Please find the attached PDF document for detailed instructions. If you experience any issues or have any questions, please contact our representative, Jim Fritz at james.l.fritz@boeing.com. ");
        emailMessageBody.append(Constants.HTML_LINE_BREAK).append(Constants.HTML_LINE_BREAK);
        emailMessageBody.append(Constants.HTML_LINE_BREAK).append(Constants.HTML_LINE_BREAK);
        emailMessageBody.append("Thank you, ").append("FliteDeck Advisor Support");
        emailMessageBody.append(Constants.HTML_LINE_BREAK).append(Constants.HTML_LINE_BREAK);

        BufferedReader reader = null;
        BufferedWriter writer = null;

        try{
            logger.debug("NewRegistrationProcess: emailAddress [{}]", emailAddress);
            logger.debug("NewRegistrationProcess: activationCode [{}]", activationCode);
            logger.debug("NewRegistrationProcess: token length [{}]", base64EncodedPayload.length());
            logger.debug("NewRegistrationProcess: airline [{}]", airline);
            userAccountRegister.insertActivationCode(emailAddress, activationCode, base64EncodedPayload, airline);

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException io) {
                    //ignore
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException io) {
                    //ignore
                }
            }
        }

        logger.info("Registered {} using ", newUser.getUserPrincipalName(), "NewRegistrationProcess");
        return emailMessageBody.toString();
    }

    public String getOldActivationEmailForUser(User newUser, String registrationToken)
            throws UserAccountRegistrationException {
        logger.debug("asldkfja;sdlkf");
        logger.debug(" ===========FDAClientCert=======");
        String fdadvisorClientCertBase64 = new StringBuilder(appProps.get("FDAdvisorClientCertName")).append("base64").toString();
        logger.info("CERT IS: {}", fdadvisorClientCertBase64);

        String base64EncodedPayload = Base64.getEncoder().encodeToString(
                new StringBuilder(newUser.getUserPrincipalName()).append(' ').append(registrationToken)
                        .append(' ').append(this.appProps.get(fdadvisorClientCertBase64)).toString().getBytes());

        StringBuilder emailMessageBody = new StringBuilder();
        String airline = newUser.getGroups().stream()
                .filter(group -> group.getDisplayName().startsWith("airline-"))
                .map(group -> group.getDisplayName().replace("airline-", StringUtils.EMPTY).toUpperCase())
                .collect(Collectors.joining(","));
        emailMessageBody.append("Hi ").append(newUser.getDisplayName()).append(' ')
                .append(String.format("(%s),", airline));

        String role = newUser.getGroups().stream()
                .filter(group -> group.getDisplayName().startsWith("role-"))
                .map(group -> group.getDisplayName().replace("role-airline", StringUtils.EMPTY).toLowerCase())
                .collect(Collectors.joining(","));

        emailMessageBody.append(Constants.HTML_LINE_BREAK).append(Constants.HTML_LINE_BREAK);
        emailMessageBody.append("Your new account for FliteDeck Advisor has been successfully created. The Airline ").append(String.format("%s",
                StringUtils.capitalize(role))).append( " role is assigned to your account.");

        emailMessageBody.append(Constants.HTML_LINE_BREAK).append(Constants.HTML_LINE_BREAK);
        emailMessageBody.append("To get started with your new account registration,");

        emailMessageBody.append(Constants.HTML_LINE_BREAK).append(Constants.HTML_LINE_BREAK);
        emailMessageBody.append("   1. Go to the <a href=\"https://itunes.apple.com/us/app/flitedeck-advisor/id1058617698\">App Store</a>")
                .append(" to install FliteDeck Advisor on your iPad. Open the installed application.").append(Constants.HTML_LINE_BREAK);
        emailMessageBody.append("   2. Come back to this email and tap on the MP attachment to open it. Tap on the icon at the top-right corner")
                .append(" of the new screen, then tap on \"Copy to FliteDeck Advisor\" to continue.").append(Constants.HTML_LINE_BREAK);
        emailMessageBody.append("   3. After completing the registration and the WiFi configuration, reopen the FliteDeck Advisor to start using it.");

        emailMessageBody.append(Constants.HTML_LINE_BREAK).append(Constants.HTML_LINE_BREAK);
        emailMessageBody.append("Please find the attached PDF document for detailed instructions. If you experience any issues or have any questions, please contact our representative, Jim Fritz at james.l.fritz@boeing.com. ");
        emailMessageBody.append(Constants.HTML_LINE_BREAK).append(Constants.HTML_LINE_BREAK);
        emailMessageBody.append(Constants.HTML_LINE_BREAK).append(Constants.HTML_LINE_BREAK);
        emailMessageBody.append("Thank you, ").append("FliteDeck Advisor Support");
        emailMessageBody.append(Constants.HTML_LINE_BREAK).append(Constants.HTML_LINE_BREAK);

        BufferedReader reader = null;
        BufferedWriter writer = null;

        try{
            File emailMpAttachment = new ClassPathResource(appProps.get("EmailMpAttachmentLocation")).getFile();
            reader = new BufferedReader(new FileReader(emailMpAttachment));
            String str;
            StringBuffer sb = new StringBuffer();
            while ((str = reader.readLine()) != null) {
                str = str.replaceAll("<!-- Add Certificate here -->", base64EncodedPayload);
                sb.append(str + "\n");
            }
            reader.close();
            String mpFileName = newUser.getDisplayName().replaceAll("\\s+", "_").toLowerCase() + ".mp";
            Path path = Files.createTempDirectory(StringUtils.EMPTY);
            File mpFile = new File(path.toString() + File.separator + mpFileName);
            writer = new BufferedWriter(new FileWriter(mpFile));
            writer.write(sb.toString());
            writer.close();
            logger.debug("upload mp file to azure blob [{}]", mpFile.getAbsolutePath());
            aadClient.uploadAttachment(mpFile);
            logger.debug("upload complete!");
        }catch(IOException io){
            logger.warn("Failed to read attachment: {}", io.getMessage(), io);
            throw new UserAccountRegistrationException(new ApiError("CREATE_USER_FAILURE", "failed to read attachment", Constants.RequestFailureReason.INTERNAL_SERVER_ERROR));
        }finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException io) {
                    //ignore
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException io) {
                    //ignore
                }
            }
        }

        logger.info("Registered {} using ", newUser.getUserPrincipalName(), "OldRegistrationProcess");
        return emailMessageBody.toString();
    }

}
