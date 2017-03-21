package won.owner.web;

import org.apache.jena.query.Dataset;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.owner.service.impl.URIService;
import won.protocol.model.ConnectionState;
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

  private static final String OWNER_REMOTE_NEED_LINK = "/#post/?postUri=";
  private static final String OWNER_CONNECTION_LINK = "/#post/?postUri=%s&connectionUri=%s&connectionType=%s";
  private static final String OWNER_LOCAL_NEED_LINK = "/#/post/?postUri=";

  private static final String SUBJECT_CONVERSATION_MESSAGE = "new message";
  private static final String SUBJECT_CONNECT = "new conversation request";
  private static final String SUBJECT_MATCH = "new match";
  private static final String SUBJECT_CLOSE = "conversation closed";

  private WonMailSender wonMailSender;

  @Autowired
  LinkedDataSource linkedDataSource;

  @Autowired
  private URIService uriService;

  private VelocityEngine velocityEngine;
  private Template conversationNotificationHtmlTemplate;
  private Template connectNotificationHtmlTemplate;
  private Template closeNotificationHtmlTemplate;
  private Template hintNotificationHtmlTemplate;

  public WonOwnerMailSender() {

    velocityEngine = new VelocityEngine();
    Properties properties = new Properties();
    properties.setProperty("resource.loader", "file");
    properties.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    velocityEngine.init(properties);
    conversationNotificationHtmlTemplate = velocityEngine.getTemplate("mail-templates/conversation-notification-html.vm");
    connectNotificationHtmlTemplate = velocityEngine.getTemplate("mail-templates/connect-notification-html.vm");
    closeNotificationHtmlTemplate = velocityEngine.getTemplate("mail-templates/close-notification-html.vm");
    hintNotificationHtmlTemplate = velocityEngine.getTemplate("mail-templates/hint-notification-html.vm");
  }

  public void setWonMailSender(WonMailSender wonMailSender) {
    this.wonMailSender = wonMailSender;
  }

  private VelocityContext createContext(String toEmail, String localNeed, String remoteNeed,
                                        String localConnection, String textMsg) {

    String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();
    Dataset needDataset =  linkedDataSource.getDataForResource(URI.create(remoteNeed));
    DefaultNeedModelWrapper remoteNeedWrapper = new DefaultNeedModelWrapper(needDataset);
    String remoteNeedTitle = remoteNeedWrapper.getTitleFromIsOrAll();

    Dataset localNeedDataset =  linkedDataSource.getDataForResource(URI.create(localNeed));
    DefaultNeedModelWrapper localNeedWrapper = new DefaultNeedModelWrapper(localNeedDataset);
    String localNeedTitle = localNeedWrapper.getTitleFromIsOrAll();
    String linkLocalNeed = ownerAppLink + OWNER_LOCAL_NEED_LINK + localNeed;
    String linkRemoteNeed = uriService.getOwnerProtocolOwnerURI() + OWNER_REMOTE_NEED_LINK + remoteNeed;
    String linkConnection = ownerAppLink + String.format(OWNER_CONNECTION_LINK,localNeed, localConnection, ConnectionState.CONNECTED.getURI().toString());

    VelocityContext velocityContext = new VelocityContext();
    velocityContext.put("linkRemoteNeed", linkRemoteNeed);
    velocityContext.put("linkLocalNeed", linkLocalNeed);
    velocityContext.put("localNeedTitle", localNeedTitle);
    velocityContext.put("remoteNeedTitle", remoteNeedTitle);
    velocityContext.put("linkConnection", linkConnection);
    velocityContext.put("textMsg", textMsg);

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
}
