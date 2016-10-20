package won.bot.framework.eventbot.action.impl.mail.send;

import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.MessageChannel;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

/**
 * Created by fsuda on 18.10.2016.
 */
public class Message2MailAction extends BaseEventBotAction {
    private String uriListName;
    private String uriMimeMessageRelationsName;
    private String mailIdUriRelationsName;
    private MessageChannel sendChannel;

    public Message2MailAction(EventListenerContext eventListenerContext, String mailIdUriRelationsName, MessageChannel sendChannel) {
        super(eventListenerContext);
        this.mailIdUriRelationsName = mailIdUriRelationsName;
        this.sendChannel = sendChannel;
    }

    @Override
    protected void doRun(Event event) throws Exception {
        if(event instanceof MessageFromOtherNeedEvent){
            Connection con = ((MessageFromOtherNeedEvent) event).getCon();

            WonMessage message = ((MessageFromOtherNeedEvent) event).getWonMessage();
            String textMessage = extractTextMessageFromWonMessage(message);
            logger.debug("textMessage " + textMessage);
            //TODO: IMPLEMENT SENDING A TEXTMESSAGE TO THE EMAIL ADDRESS GIVEN IN THE NEEDURI->MAIL RELATION
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
