package won.bot.framework.eventbot.action.impl.mail.receive;

import org.hsqldb.lib.StringUtil;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.action.impl.mail.model.ActionType;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.mail.OpenConnectionEvent;
import won.bot.framework.eventbot.event.impl.mail.CloseConnectionEvent;
import won.bot.framework.eventbot.event.impl.mail.MailCommandEvent;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;

/**
 * Created by fsuda on 18.10.2016.
 */
public class MailCommandAction  extends BaseEventBotAction {
    private String mailIdUriRelationsName;

    public MailCommandAction(EventListenerContext eventListenerContext, String mailIdUriRelationsName) {
        super(eventListenerContext);
        this.mailIdUriRelationsName = mailIdUriRelationsName;
    }

    @Override
    protected void doRun(Event event) throws Exception {
        if(event instanceof MailCommandEvent){
            MimeMessage message = ((MailCommandEvent) event).getMessage();
            EventBus bus = getEventListenerContext().getEventBus();

            try{
                String replyToMailId = getReplyToMailId(message);

                WonURI wonUri = EventBotActionUtils.getWonURIForMailId(getEventListenerContext(), mailIdUriRelationsName, replyToMailId);
                assert wonUri != null;

                ActionType actionType = determineAction(message);
                logger.debug("Executing " + actionType + " on uri: " + wonUri.getUri() + " of type " + wonUri.getType());

                switch(actionType) {
                    case CLOSE_CONNECTION:
                        bus.publish(new CloseConnectionEvent(wonUri.getUri()));
                        break;
                    case CONNECT:
                        bus.publish(new OpenConnectionEvent(wonUri.getUri()));
                        break;
                    case NO_ACTION:
                    default:
                        //INVALID COMMAND
                        logger.error("No command was given or assumed");
                        break;
                }
            }catch(Exception e){
                logger.error("no reply mail was set or found");
            }
        }
    }

    public static boolean isCommandMail(MimeMessage message){
        try{
            return !StringUtil.isEmpty(getReplyToMailId(message));
        }catch(MessagingException me){
            return false;
        }
    }

    public static String getReplyToMailId(MimeMessage message) throws MessagingException {
        return message.getHeader("In-Reply-To")[0];
    }

    private static ActionType determineAction(MimeMessage message) {
        //TODO: determine better Actions and move to MailContentExtractor
        try {
            String messageContent = (String) message.getContent();
            if (messageContent.startsWith("close") || messageContent.startsWith("deny")) {
                return ActionType.CLOSE_CONNECTION;
            }else if(messageContent.startsWith("connect")){
                return ActionType.CONNECT;
            }else {
                return ActionType.CONNECT;
            }
        }catch(MessagingException me){
            me.printStackTrace();
            return ActionType.NO_ACTION;
        }catch(IOException ioe){
            ioe.printStackTrace();
            return ActionType.NO_ACTION;
        }
    }
}
