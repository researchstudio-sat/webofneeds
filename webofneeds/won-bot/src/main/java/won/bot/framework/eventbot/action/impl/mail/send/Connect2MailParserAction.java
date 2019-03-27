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
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.model.Connection;

/**
 * Created by fsuda on 03.10.2016.
 */
public class Connect2MailParserAction extends BaseEventBotAction {
    private MessageChannel sendChannel;
    private WonMimeMessageGenerator mailGenerator;

    public Connect2MailParserAction(WonMimeMessageGenerator mailGenerator, MessageChannel sendChannel) {
        super(mailGenerator.getEventListenerContext());
        this.sendChannel = sendChannel;
        this.mailGenerator = mailGenerator;
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if (event instanceof ConnectFromOtherNeedEvent && ctx.getBotContextWrapper() instanceof MailBotContextWrapper) {
            MailBotContextWrapper botContextWrapper = (MailBotContextWrapper) ctx.getBotContextWrapper();
            Connection con = ((ConnectFromOtherNeedEvent) event).getCon();
            URI responseTo = con.getNeedURI();
            URI remoteNeedUri = con.getRemoteNeedURI();
            MimeMessage originalMail = botContextWrapper.getMimeMessageForURI(responseTo);
            logger.debug("Someone issued a connect for URI: " + responseTo + " sending a mail to the creator: "
                            + MailContentExtractor.getFromAddressString(originalMail));
            WonMimeMessage answerMessage = mailGenerator.createConnectMail(originalMail, remoteNeedUri);
            botContextWrapper.addMailIdWonURIRelation(answerMessage.getMessageID(),
                            new WonURI(con.getConnectionURI(), UriType.CONNECTION));
            sendChannel.send(new GenericMessage<>(answerMessage));
        }
    }
}
