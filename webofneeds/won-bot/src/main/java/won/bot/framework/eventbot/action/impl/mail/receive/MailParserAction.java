package won.bot.framework.eventbot.action.impl.mail.receive;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.mail.CreateNeedFromMailEvent;
import won.bot.framework.eventbot.event.impl.mail.MailCommandEvent;
import won.bot.framework.eventbot.event.impl.mail.MailReceivedEvent;

import javax.mail.internet.MimeMessage;

/**
 * Created by fsuda on 30.09.2016.
 */
public class MailParserAction extends BaseEventBotAction {

    public MailParserAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    protected void doRun(Event event) throws Exception {
        if(event instanceof MailReceivedEvent){
            EventBus bus = getEventListenerContext().getEventBus();
            MimeMessage message = ((MailReceivedEvent) event).getMessage();

            if(CreateNeedFromMailAction.isCreateMail(message)){
                bus.publish(new CreateNeedFromMailEvent(message));
            }else if(MailCommandAction.isCommandMail(message)){
                bus.publish(new MailCommandEvent(message));
            }else{
                logger.debug("UNKNOWN MAIL WILL NOT BE PROCESSED FURTHER");
            }
        }
    }
}
