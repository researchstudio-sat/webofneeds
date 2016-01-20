package won.utils.mail;

import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

/**
 * User: ypanchenko
 * Date: 23.02.2015
 */
public class WonMailSender
{

  private MailSender mailSender;
  private SimpleMailMessage templateMessage;

  public void setMailSender(MailSender mailSender) {
    this.mailSender = mailSender;
  }

  public void setTemplateMessage(SimpleMailMessage templateMessage) {
    this.templateMessage = templateMessage;
  }

  public void sendTextMessage(String toEmail, String subject, String text) {
    SimpleMailMessage msg = new SimpleMailMessage(this.templateMessage);
    msg.setSubject(subject);
    msg.setTo(toEmail);
    msg.setText(text);
    mailSender.send(msg);
  }

}
