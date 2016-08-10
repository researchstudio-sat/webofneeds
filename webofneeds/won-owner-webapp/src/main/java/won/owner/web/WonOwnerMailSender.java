package won.owner.web;

import com.hp.hpl.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.owner.service.impl.URIService;
import won.protocol.model.ConnectionState;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.utils.mail.WonMailSender;

import java.net.URI;

/**
 * User: ypanchenko
 * Date: 23.02.2015
 */
public class WonOwnerMailSender {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private WonMailSender wonMailSender;

  @Autowired
  LinkedDataSource linkedDataSource;

  private static final String OWNER_REMOTE_NEED_LINK = "/#post/?postUri=";
  private static final String OWNER_CONNECTION_LINK = "/#post/?postUri=%s&connectionUri=%s&connectionType=%s";
  private static final String OWNER_LOCAL_NEED_LINK = "/#/post/?postUri=";

  private static final String NOTIFICATION_END = "\n\n\n" +
    "Sincerely yours,\nOwner application team" +
    "\n\n\n\n" +
    "You have received this email because you are subscribed to getting " +
    "notifications for your posting. If you think we should not have sent you this email, please click on the link " +
    "below." +
    "\n" +
    "\n\n" +
    "This is an automatic email, please do not reply." ;

  private static final String NOTIFICATION_START_HTML =
    "<p>" +
    " Hi there," +
    "</p>";
  private static final String NOTIFICATION_END_HTML =
    "<br/>" +
    "<p>" +
    " <span>Best Wishes,</span>" +
    " <br/>" +
    " <span>Owner application team</span>" +
    "</p>";

  private static final String SUBJECT_CONVERSATION_MESSAGE = "new message";
  private static final String SUBJECT_CONNECT = "new conversation request";
  private static final String SUBJECT_MATCH = "new match";
  private static final String SUBJECT_CLOSE = "conversation closed";


  @Autowired
  private URIService uriService;

  public void setWonMailSender(WonMailSender wonMailSender) {
    this.wonMailSender = wonMailSender;
  }

  @Deprecated
  /**
   * @deprecated as composes email with link to the old GUI
   */
  public void sendPrivateLink(String toEmail, String privateLink) {

    String subject = "Your request posting";
    privateLink = uriService.getOwnerProtocolOwnerURI() + "/#/private-link?id=" + privateLink;
    String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();

    String text = "Dear user," +
      "\n\n" +
      "Thank you for posting your request. Your request private link is  " +
      privateLink +
      "\n\n\n" +
      "Sincerely yours,\nOwner application team" +
      "\n\n\n\n" +
      "You have received this email because you have made a posing as not signed-in user at " +
      ownerAppLink +
      ". If you received this email in error please click on the link below." +
      "\n" +
      //TODO implement the link that should close the posting..."
      "\n\n" +
      "This is an automatic email, please do not reply.";

    this.wonMailSender.sendTextMessage(toEmail, subject, text);

  }




  public void sendConversationNotificationHtmlMessage(String toEmail, String localNeed, String
    remoteNeed, String localConnection, String textMsg) {


    String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();
    Dataset needDataset =  linkedDataSource.getDataForResource(URI.create(remoteNeed));
    String remoteNeedTitle = WonRdfUtils.NeedUtils.getNeedTitle(needDataset, URI.create(remoteNeed));
    Dataset localNeedDataset =  linkedDataSource.getDataForResource(URI.create(localNeed));
    String localNeedTitle = WonRdfUtils.NeedUtils.getNeedTitle(localNeedDataset, URI.create(localNeed));
    String linkLocalNeed = ownerAppLink + OWNER_LOCAL_NEED_LINK + localNeed;
    String linkRemoteNeed = uriService.getOwnerProtocolOwnerURI() + OWNER_REMOTE_NEED_LINK + remoteNeed;
    String linkConnection = ownerAppLink + String.format(OWNER_CONNECTION_LINK,localNeed, localConnection, ConnectionState.CONNECTED.getURI().toString());

    // TODO implement login in context functionality for the linked from the email owner interface

    String body =
    "<div>" +
      NOTIFICATION_START_HTML +
      "<p>The owner of '<a href=\"" + linkRemoteNeed + "\">" + remoteNeedTitle + "</a>'" +
      "   sent you a message via " +
      "your posting <a href=\"" + linkLocalNeed + "\">"+ localNeedTitle+ "</a>. " +
      "They wrote:" +
      "</p>" +
      "<p style=\"border-left:thick solid #808080;\">" +
      "   <span style=\"margin-left:5px;color:#808080;\">" + textMsg + "</span>" +
      "</p>" +
      "<p><a href=\"" + linkConnection + "\">[Click here to answer them]</a></p>" +
      NOTIFICATION_END_HTML +
    "</div>";


    logger.debug("sending " + SUBJECT_CONVERSATION_MESSAGE + " to " + toEmail);

    this.wonMailSender.sendHtmlMessage(toEmail, SUBJECT_CONVERSATION_MESSAGE, body);

  }


  public void sendConnectNotificationHtmlMessage(String toEmail, String localNeed, String
    remoteNeed, String localConnection, String textMsg) {

    String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();
    Dataset needDataset =  linkedDataSource.getDataForResource(URI.create(remoteNeed));
    String remoteNeedTitle = WonRdfUtils.NeedUtils.getNeedTitle(needDataset, URI.create(remoteNeed));
    Dataset localNeedDataset =  linkedDataSource.getDataForResource(URI.create(localNeed));
    String localNeedTitle = WonRdfUtils.NeedUtils.getNeedTitle(localNeedDataset, URI.create(localNeed));
    String linkLocalNeed = ownerAppLink + OWNER_LOCAL_NEED_LINK + localNeed;
    String linkRemoteNeed = uriService.getOwnerProtocolOwnerURI() + OWNER_REMOTE_NEED_LINK + remoteNeed;
    String linkConnection = ownerAppLink + String.format(OWNER_CONNECTION_LINK,
                                                         localNeed, localConnection, ConnectionState.REQUEST_RECEIVED.getURI()
                                                                                                      .toString());
    // TODO implement login in context functionality for the linked from the email owner interface

    String theyWrote = "</p>";
    if (textMsg != null) {
      theyWrote = "They wrote:" +
        "</p>" +
        "<p style=\"border-left:thick solid #808080;\">" +
        "   <span style=\"margin-left:5px;color:#808080;\">" + textMsg + "</span>" +
        "</p>";
    }

    String body =
      "<div>" +
        NOTIFICATION_START_HTML +
        "<p>The owner of '<a href=\"" + linkRemoteNeed + "\">" + remoteNeedTitle + "</a>'" +
        "   wants to start a conversation with " +
        "you via your posting <a href=\"" + linkLocalNeed + "\">"+ localNeedTitle+ "</a>. " +
        theyWrote +
        "<p><a href=\"" + linkConnection + "\">[Click here to pick up the conversation]</a></p>" +
        NOTIFICATION_END_HTML +
      "</div>";


    logger.debug("sending " + SUBJECT_CONNECT + " to " + toEmail);

    this.wonMailSender.sendHtmlMessage(toEmail, SUBJECT_CONNECT, body);

  }

  public void sendCloseNotificationHtmlMessage(String toEmail, String localNeed, String
    remoteNeed, String localConnection, String textMsg) {

    String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();
    Dataset needDataset =  linkedDataSource.getDataForResource(URI.create(remoteNeed));
    String remoteNeedTitle = WonRdfUtils.NeedUtils.getNeedTitle(needDataset, URI.create(remoteNeed));
    Dataset localNeedDataset =  linkedDataSource.getDataForResource(URI.create(localNeed));
    String localNeedTitle = WonRdfUtils.NeedUtils.getNeedTitle(localNeedDataset, URI.create(localNeed));
    String linkLocalNeed = ownerAppLink + OWNER_LOCAL_NEED_LINK + localNeed;
    String linkRemoteNeed = uriService.getOwnerProtocolOwnerURI() + OWNER_REMOTE_NEED_LINK + remoteNeed;
    String linkConnection = ownerAppLink + String.format(OWNER_CONNECTION_LINK,
                                                         localNeed, localConnection, ConnectionState.CLOSED.getURI()
                                                                                                                     .toString());
    // TODO implement login in context functionality for the linked from the email owner interface

    String theyWrote = "</p>";
    if (textMsg != null) {
      theyWrote = "They wrote:" +
        "</p>" +
        "<p style=\"border-left:thick solid #808080;\">" +
        "   <span style=\"margin-left:5px;color:#808080;\">" + textMsg + "</span>" +
        "</p>";
    }

    String body =
      "<div>" +
        NOTIFICATION_START_HTML +
        "<p>The owner of '<a href=\"" + linkRemoteNeed + "\">" + remoteNeedTitle + "</a>'" +
        "   closed the conversation with " +
        "your posting <a href=\"" + linkLocalNeed + "\">"+ localNeedTitle+ "</a>. " +
        theyWrote +
        "<p><a href=\"" + linkConnection + "\">[Click here to view the conversation]</a></p>" +
        NOTIFICATION_END_HTML +
        "</div>";


    logger.debug("sending " + SUBJECT_CLOSE + " to " + toEmail);

    this.wonMailSender.sendHtmlMessage(toEmail, SUBJECT_CLOSE, body);

  }


  public void sendHintNotificationMessageHtml(String toEmail, String localNeed, String remoteNeed, String localConnection) {

    String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();
    Dataset needDataset =  linkedDataSource.getDataForResource(URI.create(remoteNeed));
    String remoteNeedTitle = WonRdfUtils.NeedUtils.getNeedTitle(needDataset, URI.create(remoteNeed));
    Dataset localNeedDataset =  linkedDataSource.getDataForResource(URI.create(localNeed));
    String localNeedTitle = WonRdfUtils.NeedUtils.getNeedTitle(localNeedDataset, URI.create(localNeed));
    String linkLocalNeed = ownerAppLink + OWNER_LOCAL_NEED_LINK + localNeed;
    String linkRemoteNeed = uriService.getOwnerProtocolOwnerURI() + OWNER_REMOTE_NEED_LINK + remoteNeed;
    String linkConnection = ownerAppLink + String.format(OWNER_CONNECTION_LINK, localNeed, localConnection,
                                                         ConnectionState.SUGGESTED.getURI().toString());
    // TODO implement login in context functionality for the linked from the email owner interface

    String body =
      "<div>" +
        NOTIFICATION_START_HTML +
        "<p>The posting '<a href=\"" + linkRemoteNeed + "\">" + remoteNeedTitle + "</a>'" +
        "   might be interesting for "+
        "your posting <a href=\"" + linkLocalNeed + "\">"+ localNeedTitle+ "</a>. " +
        "</p>" +
        "<p><a href=\"" + linkConnection + "\">[Click here to request a conversation]</a></p>" +
        NOTIFICATION_END_HTML +
        "</div>";


    logger.debug("sending " + SUBJECT_MATCH + " to " + toEmail);

    this.wonMailSender.sendHtmlMessage(toEmail, SUBJECT_MATCH, body);

  }
}
