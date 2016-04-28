package won.owner.web;

import com.hp.hpl.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.owner.service.impl.URIService;
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

  private static final String OWNER_REMOTE_NEED_LINK = "/#/post/visitor/info/?theirUri=";
  private static final String OWNER_CONNECTION_LINK = "/#/post/visitor/messages/?myUri=<>&theirUri=";
  private static final String OWNER_LOCAL_NEED_LINK = "/#/post/owner/info?myUri=";

  private static final String NOTIFICATION_END = "\n\n\n" +
    "Sincerely yours,\nOwner application team" +
    "\n\n\n\n" +
    "You have received this email because you are subscribed to getting " +
    "notifications for your posting. If you received this email in error please click on the link below." +
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

  private static final String SUBJECT_CONVERSATION_MESSAGE = "You have received a new Conversation Message";
  private static final String SUBJECT_CONNECT = "You have received a new Conversation Request";
  private static final String SUBJECT_MATCH = "You have received a new Match";


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

  @Deprecated
  /**
   * @deprecated as composes email with link to the old GUI
   */
  public void sendNotificationMessage(String toEmail, String messageType, String receiverNeed) {

    String subject = "You received a new " + messageType;
    String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();

    // the public link is used temporarily, see TODO comments below
    String publicLink = uriService.getOwnerProtocolOwnerURI() + "/#/post-detail?need=" + receiverNeed;
    // TODO when we implement login in context functionality, we should send here the link that
    // would point to the private link page of the corresponding need, the would in turn redirect
    // to sign-in dialog, and after user signs in, would display that need private link page

    String text = "Dear user," +
      "\n\n" +
      "You have received a new " +
      messageType +
      " for your posting " +
      publicLink +
      ". Please visit " +
      ownerAppLink +
      " to view it in detail." +
      "\n\n\n" +
      "Sincerely yours,\nOwner application team" +
      "\n\n\n\n" +
      "You have received this email because you have subscribed to getting " +
      messageType +
      " notifications for your posting " +
      publicLink +
      ". If you received this email in error please click on the link below." +
      "\n" +
      //TODO implement the link that should do something that makes sense..."
      "\n\n" +
      "This is an automatic email, please do not reply.";

    logger.info("sending " + subject + " to " + toEmail);

    this.wonMailSender.sendTextMessage(toEmail, subject, text);

  }

  public void sendHintNotificationMessage(String toEmail, String localNeed, String remoteNeed) {

    String subject = "You have received a new Match";
    String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();

    String linkLocalNeed = uriService.getOwnerProtocolOwnerURI() + OWNER_LOCAL_NEED_LINK + localNeed;
    String linkMatch = uriService.getOwnerProtocolOwnerURI() + OWNER_REMOTE_NEED_LINK + remoteNeed;
    // TODO implement login in context functionality for the linked from the email owner interface

    String text = "Dear User," +
      "\n\n" +
      subject +
      " for your posting " +
      linkLocalNeed +
      ". Please visit " +
      linkMatch +
      " to view it in detail." +
      NOTIFICATION_END
      ;

    logger.info("sending " + subject + " to " + toEmail);

    this.wonMailSender.sendTextMessage(toEmail, subject, text);

  }

  public void sendConversationNotificationMessage(String toEmail, String msgType, String localNeed, String
    remoteNeed) {

    String subject = "You have received a new Conversation " + msgType;
    String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();

    String linkLocalNeed = uriService.getOwnerProtocolOwnerURI() + OWNER_LOCAL_NEED_LINK + localNeed;
    String linkConnection = uriService.getOwnerProtocolOwnerURI() + OWNER_CONNECTION_LINK.replaceAll("<>", localNeed)
      + remoteNeed;
    // TODO implement login in context functionality for the linked from the email owner interface

    String text = "Dear User," +
      "\n\n" +
      subject +
      " for your posting " +
      linkLocalNeed +
      ". Please visit " +
      linkConnection +
      " to view it in detail." +
      NOTIFICATION_END
      ;

    logger.info("sending " + subject + " to " + toEmail);

    this.wonMailSender.sendTextMessage(toEmail, subject, text);

  }

  public void sendConversationNotificationHtmlMessage(String toEmail, String localNeed, String
    remoteNeed, String textMsg) {


    String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();
    Dataset needDataset =  linkedDataSource.getDataForResource(URI.create(remoteNeed));
    String remoteNeedTitle = WonRdfUtils.NeedUtils.getNeedTitle(needDataset, URI.create(remoteNeed));
    String linkLocalNeed = ownerAppLink + OWNER_LOCAL_NEED_LINK + localNeed;
    String linkRemoteNeed = uriService.getOwnerProtocolOwnerURI() + OWNER_REMOTE_NEED_LINK + remoteNeed;
    String linkConnection = ownerAppLink + OWNER_CONNECTION_LINK.replaceAll("<>", localNeed)
      + remoteNeed;
    // TODO implement login in context functionality for the linked from the email owner interface

    String body =
    "<div>" +
      NOTIFICATION_START_HTML +
      "<p>The person behind '<a href=\"" + linkRemoteNeed + "\">" + remoteNeedTitle + "</a>'" +
      "   has send <a href=\"" + linkLocalNeed + "\">you</a> a message. They wrote:" +
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
    remoteNeed, String textMsg) {

    String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();
    Dataset needDataset =  linkedDataSource.getDataForResource(URI.create(remoteNeed));
    String remoteNeedTitle = WonRdfUtils.NeedUtils.getNeedTitle(needDataset, URI.create(remoteNeed));
    String linkLocalNeed = ownerAppLink + OWNER_LOCAL_NEED_LINK + localNeed;
    String linkRemoteNeed = uriService.getOwnerProtocolOwnerURI() + OWNER_REMOTE_NEED_LINK + remoteNeed;
    String linkConnection = ownerAppLink + OWNER_CONNECTION_LINK.replaceAll("<>", localNeed)
      + remoteNeed;
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
        "<p>The person behind '<a href=\"" + linkRemoteNeed + "\">" + remoteNeedTitle + "</a>'" +
        "   wants to contact <a href=\"" + linkLocalNeed + "\">you</a>. " +
        theyWrote +
        "<p><a href=\"" + linkConnection + "\">[Click here to pick up the conversation]</a></p>" +
        NOTIFICATION_END_HTML +
      "</div>";


    logger.debug("sending " + SUBJECT_CONNECT + " to " + toEmail);

    this.wonMailSender.sendHtmlMessage(toEmail, SUBJECT_CONNECT, body);

  }

  public void sendHintNotificationMessageHtml(String toEmail, String localNeed, String remoteNeed) {

    String ownerAppLink = uriService.getOwnerProtocolOwnerURI().toString();
    Dataset needDataset =  linkedDataSource.getDataForResource(URI.create(remoteNeed));
    String remoteNeedTitle = WonRdfUtils.NeedUtils.getNeedTitle(needDataset, URI.create(remoteNeed));
    String linkLocalNeed = ownerAppLink + OWNER_LOCAL_NEED_LINK + localNeed;
    String linkRemoteNeed = uriService.getOwnerProtocolOwnerURI() + OWNER_REMOTE_NEED_LINK + remoteNeed;
    String linkConnection = ownerAppLink + OWNER_CONNECTION_LINK.replaceAll("<>", localNeed)
      + remoteNeed;
    // TODO implement login in context functionality for the linked from the email owner interface

    String body =
      "<div>" +
        NOTIFICATION_START_HTML +
        "<p>The post '<a href=\"" + linkRemoteNeed + "\">" + remoteNeedTitle + "</a>'" +
        "   might be interesting for <a href=\"" + linkLocalNeed + "\">you</a>. " +
        "</p>" +
        "<p><a href=\"" + linkConnection + "\">[Click here to request a conversation]</a></p>" +
        NOTIFICATION_END_HTML +
        "</div>";


    logger.debug("sending " + SUBJECT_MATCH + " to " + toEmail);

    this.wonMailSender.sendHtmlMessage(toEmail, SUBJECT_MATCH, body);

  }
}
