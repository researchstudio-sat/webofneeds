package won.bot.framework.eventbot.action.impl.mail.send;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.action.impl.mail.receive.util.UriType;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.protocol.model.Connection;

import javax.mail.internet.MimeMessage;
import java.net.URI;

/**
 * Created by fsuda on 03.10.2016.
 */
public class Connect2MailParserAction extends BaseEventBotAction {
    private String uriListName;
    private String uriMailRelationsName; //MAYBE WE DO NOT NEED THIS SINCE ITS IN THE uriMimeMessageRelations already (within the MimeMessage)
    private String uriMimeMessageRelationsName;
    private String mailIdUriRelationsName;
    private MessageChannel messageChannel;

    public Connect2MailParserAction(EventListenerContext eventListenerContext, String uriListName, String uriMailRelationsName, String uriMimeMessageRelationsName, String mailIdUriRelationsName, MessageChannel messageChannel) {
        super(eventListenerContext);
        this.uriListName = uriListName;
        this.uriMailRelationsName = uriMailRelationsName;
        this.uriMimeMessageRelationsName = uriMimeMessageRelationsName;
        this.mailIdUriRelationsName = mailIdUriRelationsName;
        this.messageChannel = messageChannel;
    }

    @Override
    protected void doRun(Event event) throws Exception {
        if (event instanceof ConnectFromOtherNeedEvent) {
            Connection con = ((ConnectFromOtherNeedEvent) event).getCon();

            URI responseTo = con.getNeedURI();
            String respondToMailAddress = EventBotActionUtils.getAddressForURI(getEventListenerContext(), uriMailRelationsName, responseTo);
            logger.debug("Someone issued a connect for URI: " + responseTo + " sending a mail to the creator: " + respondToMailAddress);

            MimeMessage originalMail = EventBotActionUtils.getMimeMessageForURI(getEventListenerContext(), uriMimeMessageRelationsName, responseTo);
            MimeMessage answerMessage = (MimeMessage) originalMail.reply(false);

            StringBuilder mailText = new StringBuilder("Someone wants to connect with you:\n");
            mailText.append("{");
            mailText.append(con.getRemoteNeedURI());
            mailText.append("}\n\n");

            mailText.append("Original Message from <");
            mailText.append(respondToMailAddress);
            mailText.append("> on ");
            mailText.append(originalMail.getSentDate());
            mailText.append(":\n");
            String originalContent = ">"+originalMail.getContent().toString().replaceAll("\\n","\n>");
            mailText.append(originalContent);
            answerMessage.setText(mailText.toString());

            //We need to create an instance of our own MimeMessage Implementation in order to have the Unique Message Id set before sending
            WonMimeMessage answerMessage2 = new WonMimeMessage(answerMessage);
            answerMessage2.updateMessageID();

            EventBotActionUtils.addMailIdWonURIRelation(getEventListenerContext(), mailIdUriRelationsName, answerMessage2.getMessageID(), new WonURI(con.getConnectionURI(), UriType.CONNECTION));

            messageChannel.send(new GenericMessage<>(answerMessage2));
        }
    }
}
