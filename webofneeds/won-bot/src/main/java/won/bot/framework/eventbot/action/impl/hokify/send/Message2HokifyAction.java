package won.bot.framework.eventbot.action.impl.hokify.send;

import org.apache.jena.rdf.model.Model;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import won.bot.framework.bot.context.HokifyJobBotContextWrapper;
import won.bot.framework.bot.context.TelegramBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.hokify.WonHokifyJobBotHandler;
import won.bot.framework.eventbot.action.impl.mail.model.UriType;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.action.impl.telegram.WonTelegramBotHandler;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * Created by MS on 24.09.2018.
 */
public class Message2HokifyAction extends BaseEventBotAction {

    WonHokifyJobBotHandler wonHokifyBotHandler;

    public Message2HokifyAction(EventListenerContext ctx, WonHokifyJobBotHandler wonHokifyBotHandler) {
        super(ctx);
        this.wonHokifyBotHandler = wonHokifyBotHandler;
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        
        logger.info("MessageEvent received");
        
        EventListenerContext ctx = getEventListenerContext();

        if(event instanceof MessageFromOtherNeedEvent && ctx.getBotContextWrapper() instanceof HokifyJobBotContextWrapper) {
            HokifyJobBotContextWrapper botContextWrapper = (HokifyJobBotContextWrapper) ctx.getBotContextWrapper();

            Connection con = ((MessageFromOtherNeedEvent) event).getCon();
            WonMessage wonMessage =((MessageFromOtherNeedEvent) event).getWonMessage();

            URI yourNeedUri = con.getNeedURI();
            URI remoteNeedUri = con.getRemoteNeedURI();
            String jobURL = botContextWrapper.getJobURLForURI(yourNeedUri);
            if(jobURL == null) {
                logger.error("No JobURL found for the specified needUri");
                return;
            }

            try{
                //TODO
                String respondWith = "No informations so far;

                Model messageModel = WonRdfUtils.MessageUtils.textMessage(respondWith);
                //TODO: Create Message that tells the other side which preconditions(shapes) are not yet met in a better way and not just by pushing a string into the conversation
                getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, messageModel));
                
                //Message message = wonHokifyBotHandler.getHokifyMessageGenerator().getConnectionTextMessage(remoteNeedUri, yourNeedUri, wonMessage));
                //botContextWrapper.addMessageIdWonURIRelation(message.getMessageId(), new WonURI(con.getConnectionURI(), UriType.CONNECTION));
            }catch (Exception te){
                logger.error(te.getMessage());
            }
        }
    }
}
