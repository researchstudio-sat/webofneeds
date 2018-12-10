package won.owner.web;

import java.io.StringWriter;
import java.net.URI;
import java.util.Properties;

import org.apache.jena.query.Dataset;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.implement.EscapeHtmlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import won.owner.model.EmailVerificationToken;
import won.owner.model.User;
import won.owner.service.impl.URIService;
import won.protocol.util.DefaultNeedModelWrapper;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.utils.mail.WonMailSender;

/**
 * User: ypanchenko
 * Date: 23.02.2015
 */
public class WonOwnerMailSender {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String OWNER_REMOTE_NEED_LINK = "/#!post/?postUri=";
    private static final String OWNER_CONNECTION_LINK = "/#!connections?connectionUri=%s";
    private static final String OWNER_LOCAL_NEED_LINK = "/#!connections?postUri=";

    private static final String OWNER_VERIFICATION_LINK = "/#!/connections?token=";

    private static final String OWNER_ANONYMOUS_LINK = "/#!/connections?privateId=";

    private static final String SUBJECT_CONVERSATION_MESSAGE = "New message";
    private static final String SUBJECT_CONNECT = "New conversation request";
    private static final String SUBJECT_MATCH = "New match";
    private static final String SUBJECT_CLOSE = "Conversation closed";
    private static final String SUBJECT_SYSTEM_CLOSE = "Conversation closed by system";
    private static final String SUBJECT_NEED_MESSAGE = "Notification from WoN node";
    private static final String SUBJECT_SYSTEM_DEACTIVATE = "Posting deactivated by system";
    private static final String SUBJECT_VERIFICATION = "Please Verify your E-Mail Address";
    private static final String SUBJECT_ANONYMOUSLINK = "Anonymous Link";

    private WonMailSender wonMailSender;
    
    @Value(value = "${uri.prefix}")
    private URI ownerWebappUri;

    @Autowired
    LinkedDataSource linkedDataSource;

    @Autowired
    private URIService uriService;

    private VelocityEngine velocityEngine;
    private Template conversationNotificationTemplate;
    private Template connectNotificationTemplate;
    private Template closeNotificationTemplate;
    private Template systemCloseNotificationTemplate;
    private Template hintNotificationTemplate;
    private Template needMessageNotificationTemplate;
    private Template systemDeactivateNotificationTemplate;
    private Template verificationTemplate;
    private Template anonymousTemplate;

    public WonOwnerMailSender() {
        velocityEngine = new VelocityEngine();
        Properties properties = new Properties();
        properties.setProperty("resource.loader", "file");
        properties.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngine.init(properties);
        conversationNotificationTemplate = velocityEngine.getTemplate("mail-templates/conversation-notification.vm");
        connectNotificationTemplate = velocityEngine.getTemplate("mail-templates/connect-notification.vm");
        closeNotificationTemplate = velocityEngine.getTemplate("mail-templates/close-notification.vm");
        systemCloseNotificationTemplate = velocityEngine.getTemplate("mail-templates/systemclose-notification.vm");
        hintNotificationTemplate = velocityEngine.getTemplate("mail-templates/hint-notification.vm");
        needMessageNotificationTemplate = velocityEngine.getTemplate("mail-templates/needmessage-notification.vm");
        systemDeactivateNotificationTemplate = velocityEngine.getTemplate("mail-templates/system-deactivate-notification.vm");
        verificationTemplate = velocityEngine.getTemplate("mail-templates/verification.vm");
        anonymousTemplate = velocityEngine.getTemplate("mail-templates/anonymous.vm");
    }

    public void setWonMailSender(WonMailSender wonMailSender) {
        this.wonMailSender = wonMailSender;
    }

    private String useValueOrDefaultValue(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

    private VelocityContext createContext(String toEmail, String localNeed, String remoteNeed,
                                          String localConnection, String textMsg) {

        String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();
        VelocityContext velocityContext = new VelocityContext();
        EventCartridge ec = new EventCartridge();
        ec.addEventHandler(new EscapeHtmlReference());
        ec.attachToContext(velocityContext);

        if (remoteNeed != null) {
            Dataset needDataset = linkedDataSource.getDataForResource(URI.create(remoteNeed));
            DefaultNeedModelWrapper remoteNeedWrapper = new DefaultNeedModelWrapper(needDataset);
            String remoteNeedTitle = remoteNeedWrapper.getSomeTitleFromIsOrAll("en", "de");
            remoteNeedTitle = useValueOrDefaultValue(remoteNeedTitle, "(no title)");
            velocityContext.put("remoteNeedTitle", remoteNeedTitle);
            String linkRemoteNeed = uriService.getOwnerProtocolOwnerURI() + OWNER_REMOTE_NEED_LINK + remoteNeed;
            velocityContext.put("linkRemoteNeed", linkRemoteNeed);
        }

        if (localNeed != null) {
            Dataset localNeedDataset = linkedDataSource.getDataForResource(URI.create(localNeed));
            DefaultNeedModelWrapper localNeedWrapper = new DefaultNeedModelWrapper(localNeedDataset);
            String localNeedTitle = localNeedWrapper.getSomeTitleFromIsOrAll("en", "de");
            localNeedTitle = useValueOrDefaultValue(localNeedTitle, "(no title)");
            String linkLocalNeed = ownerAppLink + OWNER_LOCAL_NEED_LINK + localNeed;
            velocityContext.put("linkLocalNeed", linkLocalNeed);
            velocityContext.put("localNeedTitle", localNeedTitle);
        }

        if (localConnection != null) {
            String linkConnection = ownerAppLink + String.format(OWNER_CONNECTION_LINK, localConnection);
            velocityContext.put("linkConnection", linkConnection);
        }

        if (textMsg != null) {
            velocityContext.put("textMsg", textMsg);
        }
        
        if (this.ownerWebappUri != null) {
            velocityContext.put("serviceName", this.ownerWebappUri);
        }

        return velocityContext;
    }

    private VelocityContext createVerificationContext(EmailVerificationToken verificationToken) {
        String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();
        VelocityContext velocityContext = new VelocityContext();
        EventCartridge ec = new EventCartridge();
        ec.addEventHandler(new EscapeHtmlReference());
        ec.attachToContext(velocityContext);

        String verificationLinkUrl = ownerAppLink + OWNER_VERIFICATION_LINK + verificationToken.getToken();
        velocityContext.put("verificationLinkUrl", verificationLinkUrl);
        velocityContext.put("expirationDate", verificationToken.getExpiryDate());
        velocityContext.put("gracePeriodInHours", User.GRACEPERIOD_INHOURS);
        if (this.ownerWebappUri != null) {
            velocityContext.put("serviceName", this.ownerWebappUri);
        }

        return velocityContext;
    }

    private VelocityContext createAnonymousLinkContext(String privateId) {
        String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();
        VelocityContext velocityContext = new VelocityContext();
        EventCartridge ec = new EventCartridge();
        ec.addEventHandler(new EscapeHtmlReference());
        ec.attachToContext(velocityContext);

        String anonymousLinkUrl = ownerAppLink + OWNER_ANONYMOUS_LINK + privateId;
        velocityContext.put("anonymousLinkUrl", anonymousLinkUrl);

        return velocityContext;
    }
    
    public void sendConversationNotificationMessage(String toEmail, String localNeed, String
            remoteNeed, String localConnection, String textMsg) {

        if (textMsg != null && !textMsg.isEmpty()) {
            StringWriter writer = new StringWriter();
            VelocityContext context = createContext(toEmail, localNeed, remoteNeed, localConnection, textMsg);
            conversationNotificationTemplate.merge(context, writer);
            logger.debug("sending " + SUBJECT_CONVERSATION_MESSAGE + " to " + toEmail);
            this.wonMailSender.sendTextMessage(toEmail, SUBJECT_CONVERSATION_MESSAGE, writer.toString());
        } else {
            logger.warn("do not send notification conversation email to {} with empty message. Connection is: {}",
                    toEmail, localConnection);
        }
    }

    public void sendConnectNotificationMessage(String toEmail, String localNeed, String
            remoteNeed, String localConnection, String textMsg) {

        StringWriter writer = new StringWriter();
        VelocityContext context = createContext(toEmail, localNeed, remoteNeed, localConnection, textMsg);
        connectNotificationTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_CONNECT + " to " + toEmail);
        this.wonMailSender.sendTextMessage(toEmail, SUBJECT_CONNECT, writer.toString());
    }

    public void sendCloseNotificationMessage(String toEmail, String localNeed, String
            remoteNeed, String localConnection, String textMsg) {

        StringWriter writer = new StringWriter();
        VelocityContext context = createContext(toEmail, localNeed, remoteNeed, localConnection, textMsg);
        closeNotificationTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_CLOSE + " to " + toEmail);
        this.wonMailSender.sendTextMessage(toEmail, SUBJECT_CLOSE, writer.toString());
    }

    public void sendHintNotificationMessage(String toEmail, String localNeed, String remoteNeed, String localConnection) {

        StringWriter writer = new StringWriter();
        VelocityContext context = createContext(toEmail, localNeed, remoteNeed, localConnection, null);
        hintNotificationTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_MATCH + " to " + toEmail);
        this.wonMailSender.sendTextMessage(toEmail, SUBJECT_MATCH, writer.toString());
    }

    public void sendNeedMessageNotificationMessage(String toEmail, String localNeed, String textMsg) {
        StringWriter writer = new StringWriter();
        VelocityContext context = createContext(toEmail, localNeed, null, null, textMsg);
        needMessageNotificationTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_NEED_MESSAGE + " to " + toEmail);
        this.wonMailSender.sendTextMessage(toEmail, SUBJECT_NEED_MESSAGE, writer.toString());
    }

    public void sendSystemDeactivateNotificationMessage(String toEmail, String localNeed, String textMsg) {
        StringWriter writer = new StringWriter();
        VelocityContext context = createContext(toEmail, localNeed, null, null, textMsg);
        systemDeactivateNotificationTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_SYSTEM_DEACTIVATE + " to " + toEmail);
        this.wonMailSender.sendTextMessage(toEmail, SUBJECT_SYSTEM_DEACTIVATE, writer.toString());
    }

    public void sendSystemCloseNotificationMessage(String toEmail, String localNeed, String
            remoteNeed, String localConnection, String textMsg) {

        StringWriter writer = new StringWriter();
        VelocityContext context = createContext(toEmail, localNeed, remoteNeed, localConnection, textMsg);
        systemCloseNotificationTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_SYSTEM_CLOSE + " to " + toEmail);
        this.wonMailSender.sendTextMessage(toEmail, SUBJECT_SYSTEM_CLOSE, writer.toString());
    }

    public void sendVerificationMessage(User user, EmailVerificationToken verificationToken) {
        StringWriter writer = new StringWriter();
        VelocityContext context = createVerificationContext(verificationToken);
        verificationTemplate.merge(context, writer);
        logger.debug("sending "+ SUBJECT_VERIFICATION + " to " + user.getEmail());
        this.wonMailSender.sendTextMessage(user.getEmail(), SUBJECT_VERIFICATION, writer.toString());
    }

    public void sendAnonymousLinkMessage(String email, String privateId) {
        StringWriter writer = new StringWriter();
        VelocityContext context = createAnonymousLinkContext(privateId);
        anonymousTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_ANONYMOUSLINK + " to " + email);
        this.wonMailSender.sendTextMessage(email, SUBJECT_ANONYMOUSLINK, writer.toString());        
    }

/*
  Dead main method code. Useful for trying out velocity stuff when needed.

  public static void main(String... args){
    VelocityEngine velocityEngine = new VelocityEngine();
    Properties properties = new Properties();
    properties.setProperty("resource.loader", "file");
    properties.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    velocityEngine.init(properties);
    Template template = velocityEngine.getTemplate("mail-templates/conversation-notification.vm");
    StringWriter writer = new StringWriter();
    VelocityContext context = new VelocityContext();
    EventCartridge ec = new EventCartridge();
    ec.addEventHandler(new EscapeHtmlReference());
    ec.attachToContext( context );

    context.put("linkRemoteNeed", "https://satvm02.researchstudio.at/owner/#!/post/?postUri=https:%2F%2Fsatvm02.researchstudio.at%2Fwon%2Fresource%2Fneed%2F8772930375045372000");
    context.put("remoteNeedTitle", "höhöhö");
    context.put("linkLocalNeed", "https://satvm02.researchstudio.at/owner/#!/post/?postUri=https:%2F%2Fsatvm02.researchstudio.at%2Fwon%2Fresource%2Fneed%2F8772930375045372000");
    context.put("localNeedTitle", "Ich & ich");
    context.put("linkConnection", "https://satvm02.researchstudio.at/owner/#!/post/?postUri=https:%2F%2Fsatvm02.researchstudio.at%2Fwon%2Fresource%2Fneed%2F8772930375045372000");
    context.put("textMsg", "Hä? & was soll das jetzt? <script language=\"JavaScript\"> alert('hi') </script>");
    template.merge(context, writer);
    System.out.println(writer.toString());
  }
  */
}
