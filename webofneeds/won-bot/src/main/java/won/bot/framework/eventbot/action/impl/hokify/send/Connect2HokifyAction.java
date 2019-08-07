package won.bot.framework.eventbot.action.impl.hokify.send;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.bot.context.HokifyJobBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.open.OpenCommandEvent;
import won.bot.framework.eventbot.event.impl.command.open.OpenCommandResultEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.filter.impl.CommandResultFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;

/**
 * Created by ms on 24.09.2018.
 */
public class Connect2HokifyAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public Connect2HokifyAction(EventListenerContext ctx) {
        super(ctx);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        logger.info("ConnectionEvent received");
        EventListenerContext ctx = getEventListenerContext();
        if (event instanceof ConnectFromOtherAtomEvent
                        && ctx.getBotContextWrapper() instanceof HokifyJobBotContextWrapper) {
            HokifyJobBotContextWrapper botContextWrapper = (HokifyJobBotContextWrapper) ctx.getBotContextWrapper();
            Connection con = ((ConnectFromOtherAtomEvent) event).getCon();
            URI yourAtomUri = con.getAtomURI();
            URI targetAtomUri = con.getTargetAtomURI();
            try {
                String message = "Hello!\n I found this job offer on " + "https://hokify.at";
                final OpenCommandEvent openCommandEvent = new OpenCommandEvent(con, message);
                ctx.getEventBus().subscribe(OpenCommandResultEvent.class, new ActionOnFirstEventListener(ctx,
                                new CommandResultFilter(openCommandEvent), new BaseEventBotAction(ctx) {
                                    @Override
                                    protected void doRun(Event event, EventListener executingListener)
                                                    throws Exception {
                                        OpenCommandResultEvent connectionMessageCommandResultEvent = (OpenCommandResultEvent) event;
                                        if (connectionMessageCommandResultEvent.isSuccess()) {
                                            String jobUrl = botContextWrapper.getJobURLForURI(yourAtomUri);
                                            String respondWith = jobUrl != null
                                                            ? "You need more information?\n Just follow this link: "
                                                                            + jobUrl
                                                            : "The job is no longer available, sorry!";
                                            Model messageModel = WonRdfUtils.MessageUtils.textMessage(respondWith);
                                            ctx.getEventBus().publish(
                                                            new ConnectionMessageCommandEvent(con, messageModel));
                                        } else {
                                            logger.error("FAILURERESPONSEEVENT FOR JOB PAYLOAD");
                                        }
                                    }
                                }));
                ctx.getEventBus().publish(openCommandEvent);
            } catch (Exception te) {
                logger.error(te.getMessage());
            }
        }
    }
}
