package won.bot.framework.eventbot.action.impl.wonmessage;

import com.hp.hpl.jena.query.Dataset;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.mail.CloseConnectionEvent;
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
    String farewellMessage = "NO FAREWELL MESSAGE SET";

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
                    logger.debug("closing connection {}", connectionURI);
                    getEventListenerContext().getWonMessageSender().sendWonMessage(createWonMessage(connectionURI));
                } else {
                    logger.warn("could not determine which connection to close for event {}", event);
                }
            } catch (Exception e) {
                logger.warn("error trying to close connection", e);
            }
        }
    }

    private WonMessage createWonMessage(URI connectionURI) throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();

        Dataset connectionRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(connectionURI);
        URI remoteNeed = WonRdfUtils.NeedUtils.getRemoteNeedURIFromConnection(connectionRDF, connectionURI);
        URI localNeed = WonRdfUtils.NeedUtils.getLocalNeedURIFromConnection(connectionRDF, connectionURI);
        URI wonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromConnection(connectionRDF, connectionURI);
        Dataset remoteNeedRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(remoteNeed);

        try {
            URI remoteConnection = WonRdfUtils.NeedUtils.getRemoteConnectionURIFromConnection(connectionRDF, connectionURI);

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

            //TODO: IMPLEMENT CORRECT MESSAGE FOR CLOSE OF HINT
            return WonMessageBuilder.setMessagePropertiesForLocalOnlyClose(
                    wonNodeInformationService.generateEventURI(wonNode),
                    connectionURI,
                    localNeed,
                    wonNode
            ).build();
        }
    }
}
