package won.bot.framework.eventbot.action.impl.mail;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.needlifecycle.AbstractCreateNeedAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.protocol.model.Match;

import javax.mail.internet.MimeMessage;
import java.net.URI;

/**
 * Created by fsuda on 03.10.2016.
 */
public class Hint2MailParserAction extends BaseEventBotAction {
    private String uriListName;
    private String uriMailRelationsName;
    private MessageChannel messageChannel;

    public Hint2MailParserAction(EventListenerContext eventListenerContext, String uriListName, String uriMailRelationsName, MessageChannel messageChannel) {
        super(eventListenerContext);
        this.uriListName = uriListName;
        this.uriMailRelationsName = uriMailRelationsName;
        this.messageChannel = messageChannel;
    }

    @Override
    protected void doRun(Event event) throws Exception {
        if(event instanceof HintFromMatcherEvent){
            Match match = ((HintFromMatcherEvent) event).getMatch();
            URI responseTo = match.getFromNeed();
            String respondToMailAddress = EventBotActionUtils.getAddressForURI(getEventListenerContext(), uriMailRelationsName, responseTo);

            logger.info("Found a hint for URI: "+responseTo+" sending a mail to the creator: "+respondToMailAddress);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("mail2won@gmail.com");
            message.setTo(respondToMailAddress);
            message.setSubject("FOUND A MATCH FOR YOU");
            message.setText("We found a match for your uri: "+responseTo+" it is under the uri: "+match.getToNeed());

            Message<SimpleMailMessage> sendMessage = new GenericMessage<SimpleMailMessage>(message);
            messageChannel.send(sendMessage);
        }
    }
}
