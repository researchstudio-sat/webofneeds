package won.bot.framework.eventbot.action.impl.mail.send;

import java.net.URI;

import javax.mail.internet.MimeMessage;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import won.bot.framework.bot.context.MailBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.mail.model.UriType;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.action.impl.mail.receive.MailContentExtractor;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.model.Connection;

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
        EventListenerContext ctx = getEventListenerContext();
        if(event instanceof MessageFromOtherNeedEvent && ctx.getBotContextWrapper() instanceof MailBotContextWrapper){
            MailBotContextWrapper botContextWrapper = (MailBotContextWrapper) ctx.getBotContextWrapper();
            Connection con = ((MessageFromOtherNeedEvent) event).getCon();

            URI responseTo = con.getNeedURI();
            URI remoteNeedUri = con.getRemoteNeedURI();

            MimeMessage originalMail = botContextWrapper.getMimeMessageForURI(responseTo);
            logger.debug("Someone sent a message for URI: " + responseTo + " sending a mail to the creator: " + MailContentExtractor.getFromAddressString(originalMail));

            WonMimeMessage answerMessage = mailGenerator.createMessageMail(originalMail, responseTo, remoteNeedUri, con.getConnectionURI());
            botContextWrapper.addMailIdWonURIRelation(answerMessage.getMessageID(), new WonURI(con.getConnectionURI(), UriType.CONNECTION));

            sendChannel.send(new GenericMessage<>(answerMessage));
        }else{
            logger.debug("event was not of type MessageFromOtherNeedEvent");
        }
    }
}
