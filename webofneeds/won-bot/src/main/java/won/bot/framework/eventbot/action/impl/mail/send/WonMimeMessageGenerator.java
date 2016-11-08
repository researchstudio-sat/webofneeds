package won.bot.framework.eventbot.action.impl.mail.send;

import com.hp.hpl.jena.query.Dataset;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.mail.receive.MailContentExtractor;
import won.protocol.message.WonMessage;
import won.protocol.model.BasicNeedType;
import won.protocol.util.WonRdfUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;

/**
 * This Class is used to generate all Mails that are going to be sent via the Mail2WonBot
 */
public class WonMimeMessageGenerator {
    @Autowired
    private VelocityEngine velocityEngine;

    private EventListenerContext eventListenerContext;


    /**
     * Creates Response Message that is sent when a need tries to connect with another need
     */
    public WonMimeMessage createConnectMail(MimeMessage msgToRespondTo, URI remoteNeedUri) throws MessagingException, IOException {
        VelocityContext velocityContext = putDefaultContent(msgToRespondTo, remoteNeedUri);

        StringWriter writer = new StringWriter();

        velocityEngine.getTemplate("mail-templates/connect-mail.vm").merge(velocityContext, writer);
        return generateWonMimeMessage(msgToRespondTo, writer.toString(), remoteNeedUri);
    }

    public WonMimeMessage createHintMail(MimeMessage msgToRespondTo, URI remoteNeedUri) throws MessagingException, IOException {
        VelocityContext velocityContext = putDefaultContent(msgToRespondTo, remoteNeedUri);

        StringWriter writer = new StringWriter();

        velocityEngine.getTemplate("mail-templates/hint-mail.vm").merge(velocityContext, writer);
        return generateWonMimeMessage(msgToRespondTo, writer.toString(), remoteNeedUri);
    }

    public WonMimeMessage createMessageMail(MimeMessage msgToRespondTo, URI remoteNeedUri, URI connectionUri, WonMessage wonMessage) throws MessagingException, IOException {
        VelocityContext velocityContext = putDefaultContent(msgToRespondTo, remoteNeedUri);

        velocityContext.put("message", extractTextMessageFromWonMessage(wonMessage));

        StringWriter writer = new StringWriter();

        velocityEngine.getTemplate("mail-templates/message-mail.vm").merge(velocityContext, writer);
        return generateWonMimeMessage(msgToRespondTo, writer.toString(), remoteNeedUri);
    }

    /**
     * Creates the DefaultContext and fills in all relevant Infos which are used in every Mail, in our Case this is the NeedInfo and the quoted Original Message
     * @param msgToRespondTo Message that the new Mail is in ResponseTo (to extract the mailtext)
     * @param remoteNeedUri To extract the corresponding Need Data
     * @return VelocityContext that has prefilled all the necessary Data
     * @throws IOException
     * @throws MessagingException
     */
    private VelocityContext putDefaultContent(MimeMessage msgToRespondTo, URI remoteNeedUri) throws IOException, MessagingException {
        VelocityContext velocityContext = new VelocityContext();

        putRemoteNeedInfo(velocityContext, remoteNeedUri);
        putQuotedMessage(velocityContext, msgToRespondTo);

        return velocityContext;
    }

    private WonMimeMessage generateWonMimeMessage(MimeMessage msgToRespondTo, String mailBody, URI remoteNeedUri) throws MessagingException {
        Dataset remoteNeedRDF = eventListenerContext.getLinkedDataSource().getDataForResource(remoteNeedUri);

        MimeMessage answerMessage = (MimeMessage) msgToRespondTo.reply(false);
        answerMessage.setText(mailBody);
        answerMessage.setSubject(answerMessage.getSubject() + " <-> ["+ BasicNeedType.fromURI(WonRdfUtils.NeedUtils.getBasicNeedType(remoteNeedRDF))+"] " +  WonRdfUtils.NeedUtils.getNeedTitle(remoteNeedRDF));// TODO: Include Human Readable Basic Need Type

        //We need to create an instance of our own MimeMessage Implementation in order to have the Unique Message Id set before sending
        WonMimeMessage wonAnswerMessage = new WonMimeMessage(answerMessage);
        wonAnswerMessage.updateMessageID();

        return  wonAnswerMessage;
    }

    /**
     * Responsible for filling inc/remote-need-info.vm template
     * @param velocityContext
     * @param remoteNeedUri
     */
    private void putRemoteNeedInfo(VelocityContext velocityContext, URI remoteNeedUri) {
        Dataset remoteNeedRDF = eventListenerContext.getLinkedDataSource().getDataForResource(remoteNeedUri);

        velocityContext.put("remoteNeedTitle", WonRdfUtils.NeedUtils.getNeedTitle(remoteNeedRDF).replaceAll("\\n", "\n>"));
        velocityContext.put("remoteNeedDescription", WonRdfUtils.NeedUtils.getNeedDescription(remoteNeedRDF).replaceAll("\\n", "\n>"));

        List<String> tags = WonRdfUtils.NeedUtils.getTags(remoteNeedRDF);
        velocityContext.put("remoteNeedTags", tags.size() > 0 ? tags : null);
        velocityContext.put("remoteNeedUri", remoteNeedUri);
    }

    /**
     * Responsible for filling inc/quoted-message.vm template
     * @param velocityContext
     * @param msgToRespondTo
     * @throws MessagingException
     * @throws IOException
     */
    private void putQuotedMessage(VelocityContext velocityContext, MimeMessage msgToRespondTo)
        throws MessagingException, IOException {
        String respondToMailAddress = MailContentExtractor.getFromAddressString(msgToRespondTo);

        velocityContext.put("respondAddress", respondToMailAddress);
        velocityContext.put("sentDate", msgToRespondTo.getSentDate());

        String mailText = MailContentExtractor.getMailText(msgToRespondTo);
        if (mailText != null) {
            velocityContext.put("respondMessage", mailText.replaceAll("\\n", "\n>"));
        }
    }

    private static String extractTextMessageFromWonMessage(WonMessage wonMessage){
        if (wonMessage == null) return null;
        String message = WonRdfUtils.MessageUtils.getTextMessage(wonMessage);
        return StringUtils.trim(message);
    }

    public void setVelocityEngine(VelocityEngine velocityEngine) {
        this.velocityEngine = velocityEngine;
    }

    public void setEventListenerContext(EventListenerContext eventListenerContext) {
        this.eventListenerContext = eventListenerContext;
    }

    public EventListenerContext getEventListenerContext() {
        return eventListenerContext;
    }
}
