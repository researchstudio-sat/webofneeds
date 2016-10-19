package won.bot.framework.eventbot.action.impl.mail.send;

import org.springframework.messaging.MessageChannel;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;

/**
 * Created by fsuda on 18.10.2016.
 */
public class Message2MailAction extends BaseEventBotAction {
    private String uriListName;
    private String uriMailRelationsName; //MAYBE WE DO NOT NEED THIS SINCE ITS IN THE uriMimeMessageRelations already (within the MimeMessage)
    private String uriMimeMessageRelationsName;
    private String mailIdUriRelationsName;
    private MessageChannel messageChannel;

    public Message2MailAction(EventListenerContext eventListenerContext, String mailIdUriRelationsName, MessageChannel messageChannel) {
        super(eventListenerContext);
        this.mailIdUriRelationsName = mailIdUriRelationsName;
        this.messageChannel = messageChannel;
    }

    @Override
    protected void doRun(Event event) throws Exception {
        if(event instanceof MessageFromOtherNeedEvent){
            ((MessageFromOtherNeedEvent) event).getCon();
            //TODO: SEND A MESSAGE VIA MAIL
        }
    }
}
