package won.bot.framework.eventbot.action.impl.mail.send;

import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.mail.model.UriType;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.action.impl.mail.receive.util.MailContentExtractor;
import won.bot.framework.eventbot.action.impl.mail.send.util.WonMimeMessageGenerator;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

import javax.mail.internet.MimeMessage;
import java.net.URI;

/**
 * Created by fsuda on 18.10.2016.
 */
public class Message2MailAction extends BaseEventBotAction {
    private String uriMimeMessageRelationsName;
    private String mailIdUriRelationsName;
    private EventListenerContext ctx;
    private MessageChannel sendChannel;

    public Message2MailAction(EventListenerContext eventListenerContext, String uriMimeMessageRelationsName, String mailIdUriRelationsName, MessageChannel sendChannel) {
        super(eventListenerContext);
        this.mailIdUriRelationsName = mailIdUriRelationsName;
        this.uriMimeMessageRelationsName = uriMimeMessageRelationsName;
        this.sendChannel = sendChannel;
        this.ctx = eventListenerContext;
    }

    @Override
    protected void doRun(Event event) throws Exception {
        if(event instanceof MessageFromOtherNeedEvent){
            Connection con = ((MessageFromOtherNeedEvent) event).getCon();
            WonMessage message = ((MessageFromOtherNeedEvent) event).getWonMessage();

            URI responseTo = con.getNeedURI();
            URI remoteNeedUri = con.getRemoteNeedURI();

            MimeMessage originalMail = EventBotActionUtils.getMimeMessageForURI(ctx, uriMimeMessageRelationsName, responseTo);
            logger.debug("Someone sent a message for URI: " + responseTo + " sending a mail to the creator: " + MailContentExtractor.getFromAddressString(originalMail));

            WonMimeMessage answerMessage = WonMimeMessageGenerator.createTextMessageMail(originalMail, remoteNeedUri, extractTextMessageFromWonMessage(message));
            EventBotActionUtils.addMailIdWonURIRelation(ctx, mailIdUriRelationsName, answerMessage.getMessageID(), new WonURI(con.getConnectionURI(), UriType.CONNECTION));

            sendChannel.send(new GenericMessage<>(answerMessage));
        }else{
            logger.debug("event was not of type MessageFromOtherNeedEvent");
        }
    }

    private String extractTextMessageFromWonMessage(WonMessage wonMessage){
        if (wonMessage == null) return null;
        String message = WonRdfUtils.MessageUtils.getTextMessage(wonMessage);
        return StringUtils.trim(message);
    }
}
