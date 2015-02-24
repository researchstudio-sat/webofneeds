package won.owner.web;

import won.utils.mail.WonMailSender;

/**
 * User: ypanchenko
 * Date: 23.02.2015
 */
public class WonOwnerMailSender {

  private WonMailSender wonMailSender;

  public void setWonMailSender(WonMailSender wonMailSender) {
    this.wonMailSender = wonMailSender;
  }

  public void sendPrivateLink(String toEmail, String privateLink) {

    String subject = "Your request posting";
    String ownerAppLink = privateLink.substring(0, privateLink.indexOf("private-link"));
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
}
