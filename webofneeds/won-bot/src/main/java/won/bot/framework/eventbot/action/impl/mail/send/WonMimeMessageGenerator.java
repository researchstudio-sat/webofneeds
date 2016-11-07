package won.bot.framework.eventbot.action.impl.mail.send;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import won.bot.framework.eventbot.action.impl.mail.receive.MailContentExtractor;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Properties;

/**
 * This Class is used to generate all Mails that are going to be sent via the Mail2WonBot
 */
public class WonMimeMessageGenerator {

  private VelocityEngine velocityEngine;
  private Template mailTemplate;

  public WonMimeMessageGenerator(String templatePath) {

    VelocityEngine velocityEngine = new VelocityEngine();
    Properties properties = new Properties();
    properties.setProperty("resource.loader", "file");
    properties.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    velocityEngine.init(properties);
    mailTemplate = velocityEngine.getTemplate(templatePath);
  }

  public WonMimeMessage createMail(MimeMessage msgToRespondTo, URI remoteNeedUri, String msgText) throws
    MessagingException, IOException {

    String respondToMailAddress = MailContentExtractor.getFromAddressString(
      msgToRespondTo);
    MimeMessage answerMessage = (MimeMessage) msgToRespondTo.reply(false);

    VelocityContext velocityContext = new VelocityContext();
    StringWriter writer = new StringWriter();

    velocityContext.put("remoteNeedUri", remoteNeedUri);
    velocityContext.put("respondAddress", respondToMailAddress);
    velocityContext.put("sentDate", msgToRespondTo.getSentDate());
    velocityContext.put("message", msgText);
    String mailText = MailContentExtractor.getMailText(msgToRespondTo);
    if (mailText != null) {
      velocityContext.put("respondMessage", mailText.replaceAll("\\n", "\n>"));
    }

    mailTemplate.merge(velocityContext, writer);
    answerMessage.setText(writer.toString());

    //We need to create an instance of our own MimeMessage Implementation in order to have the Unique Message Id set before sending
    WonMimeMessage wonAnswerMessage = new WonMimeMessage(answerMessage);
    wonAnswerMessage.updateMessageID();

    return wonAnswerMessage;
  }
}
