package won.bot.framework.eventbot.action.impl.mail.send;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import won.bot.framework.bot.context.MailBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.mail.model.UriType;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.action.impl.mail.receive.MailContentExtractor;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.model.Connection;

import javax.mail.internet.MimeMessage;
import java.lang.invoke.MethodHandles;
import java.net.URI;

/**
 * Created by fsuda on 18.10.2016.
 */
public class Message2MailAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
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
        if (event instanceof MessageFromOtherAtomEvent && ctx.getBotContextWrapper() instanceof MailBotContextWrapper) {
            MailBotContextWrapper botContextWrapper = (MailBotContextWrapper) ctx.getBotContextWrapper();
            Connection con = ((MessageFromOtherAtomEvent) event).getCon();
            URI responseTo = con.getAtomURI();
            URI targetAtomUri = con.getTargetAtomURI();
            MimeMessage originalMail = botContextWrapper.getMimeMessageForURI(responseTo);
            logger.debug("Someone sent a message for URI: " + responseTo + " sending a mail to the creator: "
                            + MailContentExtractor.getFromAddressString(originalMail));
            WonMimeMessage answerMessage = mailGenerator.createMessageMail(originalMail, responseTo, targetAtomUri,
                            con.getConnectionURI());
            botContextWrapper.addMailIdWonURIRelation(answerMessage.getMessageID(),
                            new WonURI(con.getConnectionURI(), UriType.CONNECTION));
            sendChannel.send(new GenericMessage<>(answerMessage));
        } else {
            logger.debug("event was not of type MessageFromOtherAtomEvent");
        }
    }
}
