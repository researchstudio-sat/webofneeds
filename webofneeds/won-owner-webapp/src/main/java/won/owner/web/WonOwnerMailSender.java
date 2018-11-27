package won.owner.web;

import org.apache.jena.query.Dataset;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.implement.EscapeHtmlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.owner.service.impl.URIService;
import won.protocol.util.DefaultNeedModelWrapper;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.utils.mail.WonMailSender;

import java.io.StringWriter;
import java.net.URI;
import java.util.Properties;

/**
 * User: ypanchenko
 * Date: 23.02.2015
 */
public class WonOwnerMailSender {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String OWNER_REMOTE_NEED_LINK = "/#!post/?postUri=";
    private static final String OWNER_CONNECTION_LINK = "/#!connections?connectionUri=%s";
    private static final String OWNER_LOCAL_NEED_LINK = "/#!connections?postUri=";

    private static final String SUBJECT_CONVERSATION_MESSAGE = "New message";
    private static final String SUBJECT_CONNECT = "New conversation request";
    private static final String SUBJECT_MATCH = "New match";
    private static final String SUBJECT_CLOSE = "Conversation closed";
    private static final String SUBJECT_SYSTEM_CLOSE = "Conversation closed by system";
    private static final String SUBJECT_NEED_MESSAGE = "Notification from WoN node";
    private static final String SUBJECT_SYSTEM_DEACTIVATE = "Posting deactivated by system";

    private WonMailSender wonMailSender;

    @Autowired
    LinkedDataSource linkedDataSource;

    @Autowired
    private URIService uriService;

    private VelocityEngine velocityEngine;
    private Template conversationNotificationHtmlTemplate;
    private Template connectNotificationHtmlTemplate;
    private Template closeNotificationHtmlTemplate;
    private Template systemCloseNotificationHtmlTemplate;
    private Template hintNotificationHtmlTemplate;
    private Template needMessageNotificationHtmlTemplate;
    private Template systemDeactivateNotificationHtmlTemplate;


    public WonOwnerMailSender() {

        velocityEngine = new VelocityEngine();
        Properties properties = new Properties();
        properties.setProperty("resource.loader", "file");
        properties.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngine.init(properties);
        conversationNotificationHtmlTemplate = velocityEngine.getTemplate("mail-templates/conversation-notification-html.vm");
        connectNotificationHtmlTemplate = velocityEngine.getTemplate("mail-templates/connect-notification-html.vm");
        closeNotificationHtmlTemplate = velocityEngine.getTemplate("mail-templates/close-notification-html.vm");
        systemCloseNotificationHtmlTemplate = velocityEngine.getTemplate("mail-templates/systemclose-notification-html.vm");
        hintNotificationHtmlTemplate = velocityEngine.getTemplate("mail-templates/hint-notification-html.vm");
        needMessageNotificationHtmlTemplate = velocityEngine.getTemplate("mail-templates/needmessage-notification-html.vm");
        systemDeactivateNotificationHtmlTemplate = velocityEngine.getTemplate("mail-templates/system-deactivate-notification-html.vm");
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

        return velocityContext;
    }

    public void sendConversationNotificationHtmlMessage(String toEmail, String localNeed, String
            remoteNeed, String localConnection, String textMsg) {

        if (textMsg != null && !textMsg.isEmpty()) {
            StringWriter writer = new StringWriter();
            VelocityContext context = createContext(toEmail, localNeed, remoteNeed, localConnection, textMsg);
            conversationNotificationHtmlTemplate.merge(context, writer);
            logger.debug("sending " + SUBJECT_CONVERSATION_MESSAGE + " to " + toEmail);
            this.wonMailSender.sendHtmlMessage(toEmail, SUBJECT_CONVERSATION_MESSAGE, writer.toString());
        } else {
            logger.warn("do not send notification conversation email to {} with empty message. Connection is: {}",
                    toEmail, localConnection);
        }
    }

    public void sendConnectNotificationHtmlMessage(String toEmail, String localNeed, String
            remoteNeed, String localConnection, String textMsg) {

        StringWriter writer = new StringWriter();
        VelocityContext context = createContext(toEmail, localNeed, remoteNeed, localConnection, textMsg);
        connectNotificationHtmlTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_CONNECT + " to " + toEmail);
        this.wonMailSender.sendHtmlMessage(toEmail, SUBJECT_CONNECT, writer.toString());
    }

    public void sendCloseNotificationHtmlMessage(String toEmail, String localNeed, String
            remoteNeed, String localConnection, String textMsg) {

        StringWriter writer = new StringWriter();
        VelocityContext context = createContext(toEmail, localNeed, remoteNeed, localConnection, textMsg);
        closeNotificationHtmlTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_CLOSE + " to " + toEmail);
        this.wonMailSender.sendHtmlMessage(toEmail, SUBJECT_CLOSE, writer.toString());
    }

    public void sendHintNotificationMessageHtml(String toEmail, String localNeed, String remoteNeed, String localConnection) {

        StringWriter writer = new StringWriter();
        VelocityContext context = createContext(toEmail, localNeed, remoteNeed, localConnection, null);
        hintNotificationHtmlTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_MATCH + " to " + toEmail);
        this.wonMailSender.sendHtmlMessage(toEmail, SUBJECT_MATCH, writer.toString());
    }

    public void sendNeedMessageNotificationHtmlMessage(String toEmail, String localNeed, String textMsg) {
        StringWriter writer = new StringWriter();
        VelocityContext context = createContext(toEmail, localNeed, null, null, textMsg);
        needMessageNotificationHtmlTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_NEED_MESSAGE + " to " + toEmail);
        this.wonMailSender.sendHtmlMessage(toEmail, SUBJECT_NEED_MESSAGE, writer.toString());
    }

    public void sendSystemDeactivateNotificationHtmlMessage(String toEmail, String localNeed, String textMsg) {
        StringWriter writer = new StringWriter();
        VelocityContext context = createContext(toEmail, localNeed, null, null, textMsg);
        systemDeactivateNotificationHtmlTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_SYSTEM_DEACTIVATE + " to " + toEmail);
        this.wonMailSender.sendHtmlMessage(toEmail, SUBJECT_SYSTEM_DEACTIVATE, writer.toString());
    }

    public void sendSystemCloseNotificationHtmlMessage(String toEmail, String localNeed, String
            remoteNeed, String localConnection, String textMsg) {

        StringWriter writer = new StringWriter();
        VelocityContext context = createContext(toEmail, localNeed, remoteNeed, localConnection, textMsg);
        systemCloseNotificationHtmlTemplate.merge(context, writer);
        logger.debug("sending " + SUBJECT_SYSTEM_CLOSE + " to " + toEmail);
        this.wonMailSender.sendHtmlMessage(toEmail, SUBJECT_SYSTEM_CLOSE, writer.toString());
    }

/*
  Dead main method code. Useful for trying out velocity stuff when needed.

  public static void main(String... args){
    VelocityEngine velocityEngine = new VelocityEngine();
    Properties properties = new Properties();
    properties.setProperty("resource.loader", "file");
    properties.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    velocityEngine.init(properties);
    Template template = velocityEngine.getTemplate("mail-templates/conversation-notification-html.vm");
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
