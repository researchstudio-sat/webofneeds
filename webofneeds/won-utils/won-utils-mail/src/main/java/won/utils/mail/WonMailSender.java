package won.utils.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.internet.MimeMessage;
import java.io.File;

/**
 * User: ypanchenko
 * Date: 23.02.2015
 */
public class WonMailSender {

  private JavaMailSender mailSender;
  private SimpleMailMessage templateMessage;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public void setMailSender(JavaMailSender mailSender) {
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
    try {
      mailSender.send(msg);
    } catch (MailException ex) {
      logger.warn(ex.getMessage());
    }
  }

  public void sendHtmlMessage(String toEmail, String subject, String htmlBody) {

    MimeMessage msg = mailSender.createMimeMessage();
    try {
      MimeMessageHelper helper = new MimeMessageHelper(msg, true);
      helper.setFrom(this.templateMessage.getFrom());
      helper.setSubject(subject);
      helper.setTo(toEmail);
      helper.setText(htmlBody, true);
      mailSender.send(msg);
    } catch (Exception ex) {
      logger.warn(ex.getMessage());
    }
  }

  public void sendFileMessage(String toEmail, String subject, String body, String fileName, File file) {

    MimeMessage msg = mailSender.createMimeMessage();
    try {
      MimeMessageHelper helper = new MimeMessageHelper(msg, true);
      helper.setFrom(this.templateMessage.getFrom());
      helper.setSubject(subject);
      helper.setTo(toEmail);
      helper.setText(body, false);
      helper.addAttachment(fileName, file);
      mailSender.send(msg);
    } catch (Exception ex) {
      logger.warn(ex.getMessage());
    }
  }

}
