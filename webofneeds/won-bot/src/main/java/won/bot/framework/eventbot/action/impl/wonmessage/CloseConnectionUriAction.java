package won.bot.framework.eventbot.action.impl.wonmessage;

import com.hp.hpl.jena.query.Dataset;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.mail.CloseConnectionEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * Created by fsuda on 18.10.2016.
 */
public class CloseConnectionUriAction extends BaseEventBotAction {
    private String farewellMessage = "NO FAREWELL MESSAGE SET";

    public CloseConnectionUriAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event) throws Exception {
        if(event instanceof CloseConnectionEvent) {
            logger.debug("trying to close connection related to event {}", event);
            try {
                URI connectionURI = ((CloseConnectionEvent) event).getConnectionURI();
                logger.debug("Extracted connection uri {}", connectionURI);
                if (connectionURI != null) {
                    logger.debug("trying to close connection {}", connectionURI);
                    WonMessage closeConnectionMessage = createWonMessage(connectionURI);

                    EventListener successCallback = new EventListener()
                    {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            logger.debug("successfully closed connection {}", connectionURI);
                        }
                    };

                    EventListener failureCallback = new EventListener()
                    {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            String textMessage = WonRdfUtils.MessageUtils.getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                            logger.debug("close connection failed for URI {}, original message URI {}: {}", new Object[]{connectionURI, ((FailureResponseEvent) event).getOriginalMessageURI(), textMessage});
                        }
                    };
                    EventBotActionUtils.makeAndSubscribeResponseListener(closeConnectionMessage, successCallback, failureCallback, getEventListenerContext());

                    logger.debug("registered listeners for response to message URI {}", closeConnectionMessage.getMessageURI());
                    getEventListenerContext().getWonMessageSender().sendWonMessage(closeConnectionMessage);
                    logger.debug("close connection message sent with message URI {}", closeConnectionMessage.getMessageURI());
                } else {
                    logger.warn("could not determine which connection to close for event {}", event);
                }
            } catch (Exception e) {
                logger.warn("error while trying to close connection", e);
            }
        }
    }

    private WonMessage createWonMessage(URI connectionURI) throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();

        Dataset connectionRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(connectionURI);
        URI localNeed = WonRdfUtils.NeedUtils.getLocalNeedURIFromConnection(connectionRDF, connectionURI);
        URI wonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromConnection(connectionRDF, connectionURI);

        try {
            URI remoteConnection = WonRdfUtils.NeedUtils.getRemoteConnectionURIFromConnection(connectionRDF, connectionURI);
            URI remoteNeed = WonRdfUtils.NeedUtils.getRemoteNeedURIFromConnection(connectionRDF, connectionURI);
            Dataset remoteNeedRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(remoteNeed);

            return WonMessageBuilder.setMessagePropertiesForClose(
                    wonNodeInformationService.generateEventURI(wonNode),
                    connectionURI,
                    localNeed,
                    wonNode,
                    remoteConnection,
                    remoteNeed,
                    WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(remoteNeedRDF, remoteNeed),
                    farewellMessage
            ).build();
        }catch(IncorrectPropertyCountException ex){
            logger.info("could not find remote connection must be a hint");

            //TODO: IMPLEMENT CORRECT MESSAGE FOR CLOSE OF HINT AS THIS IS CURRENTLY NOT WORKING YET
            return WonMessageBuilder.setMessagePropertiesForClose(
                    wonNodeInformationService.generateEventURI(wonNode),
                    connectionURI,
                    localNeed,
                    wonNode,
                    farewellMessage
            ).build();
        }
    }
}
