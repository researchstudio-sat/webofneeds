package won.bot.framework.eventbot.action.impl.hokify.send;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import won.bot.framework.bot.context.BotContextWrapper;
import won.bot.framework.bot.context.HokifyJobBotContextWrapper;
import won.bot.framework.bot.context.TelegramBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.mail.model.UriType;
import won.bot.framework.eventbot.action.impl.mail.model.WonURI;
import won.bot.framework.eventbot.action.impl.hokify.WonHokifyJobBotHandler;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandResultEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.command.open.OpenCommandEvent;
import won.bot.framework.eventbot.event.impl.command.open.OpenCommandResultEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.filter.impl.CommandResultFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionModelMapper;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * Created by fsuda on 03.10.2016.
 */
public class Connect2HokifyAction extends BaseEventBotAction {
    WonHokifyJobBotHandler wonHokifyBotHandler;

    public Connect2HokifyAction(EventListenerContext ctx, WonHokifyJobBotHandler wonHokifyBotHandler) {
        super(ctx);
        this.wonHokifyBotHandler = wonHokifyBotHandler;
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {

        logger.info("ConnectionEvent received");

        EventListenerContext ctx = getEventListenerContext();

        if (event instanceof ConnectFromOtherNeedEvent
                && ctx.getBotContextWrapper() instanceof HokifyJobBotContextWrapper) {
            HokifyJobBotContextWrapper botContextWrapper = (HokifyJobBotContextWrapper) ctx.getBotContextWrapper();

            Connection con = ((ConnectFromOtherNeedEvent) event).getCon();

            URI yourNeedUri = con.getNeedURI();
            URI remoteNeedUri = con.getRemoteNeedURI();

            try {
                if (con != null) {
                    String message = "Hello!";
                    // Model messageToPropose = WonRdfUtils.MessageUtils.textMessage(rideText +
                    // "\n\n" + checkOrderResponse);
                    // final ConnectionMessageCommandEvent connectionMessageCommandEvent = new
                    // ConnectionMessageCommandEvent(connection, messageToPropose);
                    final OpenCommandEvent openCommandEvent = new OpenCommandEvent(con, message);
                    ctx.getEventBus().subscribe(OpenCommandResultEvent.class,
                            new ActionOnFirstEventListener(ctx, new CommandResultFilter(openCommandEvent),
                                    new BaseEventBotAction(ctx) {
                                        @Override
                                        protected void doRun(Event event, EventListener executingListener)
                                                throws Exception {
                                            OpenCommandResultEvent connectionMessageCommandResultEvent = (OpenCommandResultEvent) event;
                                            if (connectionMessageCommandResultEvent.isSuccess()) {
                                                String respondWith = "Do you really want to have a job?";

                                                Model messageModel = WonRdfUtils.MessageUtils.textMessage(respondWith);
                                                //TODO: Create Message that tells the other side which preconditions(shapes) are not yet met in a better way and not just by pushing a string into the conversation
                                                ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, messageModel));                                                                                               
                                            } else {
                                                logger.error("FAILURERESPONSEEVENT FOR JOB PAYLOAD");
                                                // analyzeBehaviour.removePreconditionMetPending(preconditionUri);
                                                // analyzeBehaviour.addPreconditionMetError(preconditionUri);

                                                // Model errorMessage = WonRdfUtils.MessageUtils.textMessage("Extracted
                                                // Payload did not go through.\n\ntype 'recheck' to check again");
                                                // ctx.getEventBus().publish(new
                                                // ConnectionMessageCommandEvent(connection, errorMessage));
                                            }
                                        }
                                    }));

                    ctx.getEventBus().publish(openCommandEvent);
                } else {
                    logger.warn(
                            "could not send chatty message on connection {} - failed to generate Connection object from RDF",
                            con.getConnectionURI());
                }
            } catch (Exception te) {
                logger.error(te.getMessage());
            }
        }
    }
}
