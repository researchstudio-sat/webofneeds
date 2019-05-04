package won.bot.framework.eventbot.action.impl.hokify.send;

import java.net.URI;

import org.apache.jena.rdf.model.Model;

import won.bot.framework.bot.context.HokifyJobBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.hokify.WonHokifyJobBotHandler;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

/**
 * Created by MS on 24.09.2018.
 */
public class Message2HokifyAction extends BaseEventBotAction {
    WonHokifyJobBotHandler wonHokifyBotHandler;

    public Message2HokifyAction(EventListenerContext ctx) {
        super(ctx);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        logger.info("MessageEvent received");
        EventListenerContext ctx = getEventListenerContext();
        if (event instanceof MessageFromOtherAtomEvent
                        && ctx.getBotContextWrapper() instanceof HokifyJobBotContextWrapper) {
            HokifyJobBotContextWrapper botContextWrapper = (HokifyJobBotContextWrapper) ctx.getBotContextWrapper();
            Connection con = ((MessageFromOtherAtomEvent) event).getCon();
            URI yourAtomUri = con.getAtomURI();
            String jobUrl = botContextWrapper.getJobURLForURI(yourAtomUri);
            String respondWith = jobUrl != null ? "You need more information?\n Just follow this link: " + jobUrl
                            : "The job is no longer available, sorry!";
            try {
                Model messageModel = WonRdfUtils.MessageUtils.textMessage(respondWith);
                getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, messageModel));
            } catch (Exception te) {
                logger.error(te.getMessage());
            }
        }
    }
}
