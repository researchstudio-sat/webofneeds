package won.bot.framework.eventbot.action.impl.mail.send;

import java.net.URI;
import java.util.Optional;

import javax.mail.internet.MimeMessage;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import won.bot.framework.bot.context.MailBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.BotActionUtils;
import won.bot.framework.eventbot.action.impl.mail.model.UriType;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.action.impl.mail.receive.MailContentExtractor;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.MessageEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.AtomHintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SocketHintFromMatcherEvent;
import won.bot.framework.eventbot.listener.EventListener;

/**
 * Created by fsuda on 03.10.2016.
 */
public class Hint2MailParserAction extends BaseEventBotAction {
    private MessageChannel sendChannel;
    private WonMimeMessageGenerator mailGenerator;

    public Hint2MailParserAction(WonMimeMessageGenerator mailGenerator, MessageChannel sendChannel) {
        super(mailGenerator.getEventListenerContext());
        this.sendChannel = sendChannel;
        this.mailGenerator = mailGenerator;
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if ((event instanceof AtomHintFromMatcherEvent || event instanceof SocketHintFromMatcherEvent)
                        && ctx.getBotContextWrapper() instanceof MailBotContextWrapper) {
            MailBotContextWrapper botContextWrapper = (MailBotContextWrapper) ctx.getBotContextWrapper();
            Optional<URI> responseTo = BotActionUtils.getRecipientAtomURIFromHintEvent(event,
                            getEventListenerContext().getLinkedDataSource());
            Optional<URI> targetAtomUri = BotActionUtils.getTargetAtomURIFromHintEvent(event,
                            getEventListenerContext().getLinkedDataSource());
            if (!(responseTo.isPresent())) {
                logger.info("could not extract recipient atom URI from hint event {}", event);
                return;
            }
            if (!(targetAtomUri.isPresent())) {
                logger.info("could not extract target atom URI from hint event {}", event);
                return;
            }
            MimeMessage originalMail = botContextWrapper.getMimeMessageForURI(responseTo.get());
            logger.debug("Found a hint for URI: " + responseTo.get() + " sending a mail to the creator: "
                            + MailContentExtractor.getFromAddressString(originalMail));
            WonMimeMessage answerMessage = mailGenerator.createHintMail(originalMail, targetAtomUri.get());
            if (event instanceof SocketHintFromMatcherEvent) {
                botContextWrapper.addMailIdWonURIRelation(answerMessage.getMessageID(), new WonURI(
                                ((MessageEvent) event).getWonMessage().getRecipientURI(), UriType.CONNECTION));
            }
            sendChannel.send(new GenericMessage<>(answerMessage));
        }
    }
}
