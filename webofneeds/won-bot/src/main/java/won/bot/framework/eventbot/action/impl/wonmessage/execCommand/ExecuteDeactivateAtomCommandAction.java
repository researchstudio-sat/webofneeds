package won.bot.framework.eventbot.action.impl.wonmessage.execCommand;

import java.net.URI;

import org.apache.jena.query.Dataset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.deactivate.DeactivateAtomCommandEvent;
import won.bot.framework.eventbot.event.impl.command.deactivate.DeactivateAtomCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.deactivate.DeactivateAtomCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

/**
 * Created by fsuda on 17.05.2017.
 */
public class ExecuteDeactivateAtomCommandAction extends BaseEventBotAction {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ExecuteDeactivateAtomCommandAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if (!(event instanceof DeactivateAtomCommandEvent))
            return;
        DeactivateAtomCommandEvent deactivateAtomCommandEvent = (DeactivateAtomCommandEvent) event;
        EventListenerContext ctx = getEventListenerContext();
        EventBus bus = ctx.getEventBus();
        final URI atomUri = deactivateAtomCommandEvent.getAtomUri();
        Dataset atomRDF = ctx.getLinkedDataSource().getDataForResource(atomUri);
        final URI wonNodeUri = WonRdfUtils.ConnectionUtils.getWonNodeURIFromAtom(atomRDF, atomUri);
        WonNodeInformationService wonNodeInformationService = ctx.getWonNodeInformationService();
        WonMessage deactivateAtomMessage = createWonMessage(wonNodeInformationService, atomUri, wonNodeUri);
        EventListener successCallback = new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                logger.debug("atom creation successful, new atom URI is {}", atomUri);
                bus.publish(new DeactivateAtomCommandSuccessEvent(atomUri, deactivateAtomCommandEvent));
            }
        };
        EventListener failureCallback = new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                String textMessage = WonRdfUtils.MessageUtils
                                .getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                logger.debug("atom creation failed for atom URI {}, original message URI {}: {}", new Object[] {
                                atomUri, ((FailureResponseEvent) event).getOriginalMessageURI(), textMessage });
                bus.publish(new DeactivateAtomCommandFailureEvent(atomUri, deactivateAtomCommandEvent, textMessage));
            }
        };
        EventBotActionUtils.makeAndSubscribeResponseListener(deactivateAtomMessage, successCallback, failureCallback,
                        ctx);
        logger.debug("registered listeners for response to message URI {}", deactivateAtomMessage.getMessageURI());
        ctx.getWonMessageSender().sendWonMessage(deactivateAtomMessage);
        logger.debug("atom creation message sent with message URI {}", deactivateAtomMessage.getMessageURI());
    }

    private WonMessage createWonMessage(WonNodeInformationService wonNodeInformationService, URI atomURI,
                    URI wonNodeURI) throws WonMessageBuilderException {
        return WonMessageBuilder
                        .setMessagePropertiesForDeactivateFromOwner(
                                        wonNodeInformationService.generateEventURI(wonNodeURI), atomURI, wonNodeURI)
                        .build();
    }
}
