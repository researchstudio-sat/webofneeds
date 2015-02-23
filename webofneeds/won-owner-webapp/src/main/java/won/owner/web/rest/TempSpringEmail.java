package won.owner.web.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

/**
 * User: ypanchenko
 * Date: 17.02.2015
 */
public class TempSpringEmail implements TempEmailerI {

  @Autowired
  private MailSender mailSender;
  @Autowired
  private SimpleMailMessage templateMessage;

  public void setMailSender(MailSender mailSender) {
    this.mailSender = mailSender;
  }

  public void setTemplateMessage(SimpleMailMessage templateMessage) {
    this.templateMessage = templateMessage;
  }

  public void sendPrivateLink(String toEmail, String privateLink) {
    SimpleMailMessage msg = new SimpleMailMessage(this.templateMessage);
    msg.setSubject("Your need tracking");
    msg.setTo(toEmail);
    msg.setText(
      "Dear user,\n\n"
        + "Thank you for placing your need. Your private link is "
        + privateLink +
        "\n\n\nSincerely yours,\nOwner application team." +
        "\n\n\nThis is automatic e-mail, do not reply.");
    this.mailSender.send(msg);

  }

}
