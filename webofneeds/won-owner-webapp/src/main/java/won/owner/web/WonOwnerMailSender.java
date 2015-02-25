package won.owner.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.owner.service.impl.URIService;
import won.utils.mail.WonMailSender;

/**
 * User: ypanchenko
 * Date: 23.02.2015
 */
public class WonOwnerMailSender {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private WonMailSender wonMailSender;

  @Autowired
  private URIService uriService;

  public void setWonMailSender(WonMailSender wonMailSender) {
    this.wonMailSender = wonMailSender;
  }

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
}
