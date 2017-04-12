package won.bot.framework.eventbot.action.impl.mail.send;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.mail.model.UriType;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.action.impl.mail.receive.MailContentExtractor;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.model.Connection;

import javax.mail.internet.MimeMessage;
import java.net.URI;

/**
 * Created by fsuda on 18.10.2016.
 */
public class Message2MailAction extends BaseEventBotAction {
    private MessageChannel sendChannel;
    private WonMimeMessageGenerator mailGenerator;

    public Message2MailAction(WonMimeMessageGenerator mailGenerator, MessageChannel sendChannel) {
        super(mailGenerator.getEventListenerContext());
        this.sendChannel = sendChannel;
        this.mailGenerator = mailGenerator;
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if(event instanceof MessageFromOtherNeedEvent){
            Connection con = ((MessageFromOtherNeedEvent) event).getCon();

            URI responseTo = con.getNeedURI();
            URI remoteNeedUri = con.getRemoteNeedURI();

            MimeMessage originalMail = EventBotActionUtils.getMimeMessageForURI(getEventListenerContext(), responseTo);
            logger.debug("Someone sent a message for URI: " + responseTo + " sending a mail to the creator: " + MailContentExtractor.getFromAddressString(originalMail));

            WonMimeMessage answerMessage = mailGenerator.createMessageMail(originalMail, responseTo, remoteNeedUri, con.getConnectionURI());
            EventBotActionUtils.addMailIdWonURIRelation(getEventListenerContext(), answerMessage.getMessageID(), new WonURI(con.getConnectionURI(), UriType.CONNECTION));

            sendChannel.send(new GenericMessage<>(answerMessage));
        }else{
            logger.debug("event was not of type MessageFromOtherNeedEvent");
        }
    }
}
