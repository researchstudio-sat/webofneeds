package won.bot.framework.eventbot.action.impl.wonmessage;

import org.apache.jena.query.Dataset;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.mail.OpenConnectionEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.FacetType;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

public class OpenConnectionUriAction extends BaseEventBotAction {
    public OpenConnectionUriAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if(event instanceof OpenConnectionEvent) {
            logger.debug("trying to open connection related to event {}", event);
            try {
                URI connectionURI = ((OpenConnectionEvent) event).getConnectionURI();
                logger.debug("Extracted connection uri {}", connectionURI);

                if(connectionURI != null){
                    String msg = ((OpenConnectionEvent) event).getMessage();
                    WonMessage openConnectionMessage = createWonMessage(connectionURI, msg);

                    EventListener successCallback = new EventListener()
                    {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            logger.debug("successfully opened connection {}", connectionURI);
                        }
                    };

                    EventListener failureCallback = new EventListener()
                    {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            String textMessage = WonRdfUtils.MessageUtils.getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                            logger.debug("open connection failed for URI {}, original message URI {}: {}", new Object[]{connectionURI, ((FailureResponseEvent) event).getOriginalMessageURI(), textMessage});
                        }
                    };
                    EventBotActionUtils.makeAndSubscribeResponseListener(openConnectionMessage, successCallback, failureCallback, getEventListenerContext());

                    logger.debug("registered listeners for response to message URI {}", openConnectionMessage.getMessageURI());
                    getEventListenerContext().getWonMessageSender().sendWonMessage(openConnectionMessage);
                    logger.debug("open connection message sent with message URI {}", openConnectionMessage.getMessageURI());

                } else {
                    logger.warn("could not determine which connection to open for event {}", event);
                }
            } catch (Exception e) {
                logger.warn("error trying to open connection", e);
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
                wonNodeInformationService.generateEventURI(wonNode),
                FacetType.OwnerFacet.getURI(),
                localNeed,
                wonNode,
                FacetType.OwnerFacet.getURI(),
                remoteNeed,
                WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(remoteNeedRDF, remoteNeed),
                msg
        ).build();
    }
}
