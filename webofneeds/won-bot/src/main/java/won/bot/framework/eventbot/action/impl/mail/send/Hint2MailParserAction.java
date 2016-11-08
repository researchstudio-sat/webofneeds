package won.bot.framework.eventbot.action.impl.mail.send;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.mail.model.UriType;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.action.impl.mail.receive.MailContentExtractor;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.protocol.message.WonMessage;
import won.protocol.model.Match;

import javax.mail.internet.MimeMessage;
import java.net.URI;

/**
 * Created by fsuda on 03.10.2016.
 */
public class Hint2MailParserAction extends BaseEventBotAction {
    private String uriMimeMessageRelationsName;
    private String mailIdUriRelationsName;
    private MessageChannel sendChannel;
    private WonMimeMessageGenerator mailGenerator;

    public Hint2MailParserAction(WonMimeMessageGenerator mailGenerator, String uriMimeMessageRelationsName, String mailIdUriRelationsName, MessageChannel sendChannel) {
        super(mailGenerator.getEventListenerContext());
        this.uriMimeMessageRelationsName = uriMimeMessageRelationsName;
        this.mailIdUriRelationsName = mailIdUriRelationsName;
        this.sendChannel = sendChannel;
        this.mailGenerator = mailGenerator;
    }

    @Override
    protected void doRun(Event event) throws Exception {
        if (event instanceof HintFromMatcherEvent) {
            Match match = ((HintFromMatcherEvent) event).getMatch();
            WonMessage message = ((HintFromMatcherEvent) event).getWonMessage();

            URI responseTo = match.getFromNeed();
            URI remoteNeedUri = match.getToNeed();

            MimeMessage originalMail = EventBotActionUtils.getMimeMessageForURI(getEventListenerContext(), uriMimeMessageRelationsName, responseTo);
            logger.debug(
              "Found a hint for URI: " + responseTo + " sending a mail to the creator: " + MailContentExtractor
                .getFromAddressString(originalMail));

            WonMimeMessage answerMessage = mailGenerator.createHintMail(originalMail, remoteNeedUri);
            EventBotActionUtils.addMailIdWonURIRelation(getEventListenerContext(), mailIdUriRelationsName, answerMessage.getMessageID(), new WonURI(message.getReceiverURI(), UriType.CONNECTION));

            sendChannel.send(new GenericMessage<>(answerMessage));
        }
    }
}
