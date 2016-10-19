package won.bot.framework.eventbot.action.impl.wonmessage;

import com.hp.hpl.jena.query.Dataset;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.mail.OpenConnectionEvent;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

public class OpenConnectionUriAction extends BaseEventBotAction {
    public OpenConnectionUriAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event) throws Exception {
        if(event instanceof OpenConnectionEvent) {
            logger.debug("trying to close connection related to event {}", event);
            try {
                URI connectionURI = ((OpenConnectionEvent) event).getConnectionURI();
                String msg = ((OpenConnectionEvent) event).getMessage();

                logger.debug("Extracted connection uri {}", connectionURI);
                if (connectionURI != null) {
                    logger.debug("closing connection {}", connectionURI);

                    getEventListenerContext().getWonMessageSender().sendWonMessage(createWonMessage(connectionURI, msg));
                } else {
                    logger.warn("could not determine which connection to close for event {}", event);
                }
            } catch (Exception e) {
                logger.warn("error trying to close connection", e);
            }
        }
    }

    private WonMessage createWonMessage(URI connectionURI, String msg) throws WonMessageBuilderException {

        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();

        Dataset connectionRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(connectionURI);
        URI remoteNeed = WonRdfUtils.NeedUtils.getRemoteNeedURIFromConnection(connectionRDF, connectionURI);
        URI localNeed = WonRdfUtils.NeedUtils.getLocalNeedURIFromConnection(connectionRDF, connectionURI);
        URI wonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromConnection(connectionRDF, connectionURI);
        Dataset remoteNeedRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(remoteNeed);

        return WonMessageBuilder.setMessagePropertiesForConnect(
                wonNodeInformationService.generateEventURI(
                        wonNode),
                connectionURI,
                localNeed,
                wonNode,
                WonRdfUtils.NeedUtils.getRemoteConnectionURIFromConnection(connectionRDF, connectionURI),
                remoteNeed,
                WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(remoteNeedRDF, remoteNeed),
                msg
        )
                .build();
    }
}
