package won.bot.framework.eventbot.action.impl.wonmessage.execCommand;

import java.lang.invoke.MethodHandles;
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
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

/**
 * Created by fsuda on 17.05.2017.
 */
public class ExecuteDeactivateAtomCommandAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
        deactivateAtomMessage = ctx.getWonMessageSender().prepareMessage(deactivateAtomMessage);
        EventListener successCallback = event12 -> {
            logger.debug("atom creation successful, new atom URI is {}", atomUri);
            bus.publish(new DeactivateAtomCommandSuccessEvent(atomUri, deactivateAtomCommandEvent));
        };
        EventListener failureCallback = event1 -> {
            String textMessage = WonRdfUtils.MessageUtils
                            .getTextMessage(((FailureResponseEvent) event1).getFailureMessage());
            logger.debug("atom creation failed for atom URI {}, original message URI {}: {}", new Object[] {
                            atomUri, ((FailureResponseEvent) event1).getOriginalMessageURI(), textMessage });
            bus.publish(new DeactivateAtomCommandFailureEvent(atomUri, deactivateAtomCommandEvent, textMessage));
        };
        EventBotActionUtils.makeAndSubscribeResponseListener(deactivateAtomMessage, successCallback, failureCallback,
                        ctx);
        logger.debug("registered listeners for response to message URI {}", deactivateAtomMessage.getMessageURI());
        ctx.getWonMessageSender().sendMessage(deactivateAtomMessage);
        logger.debug("atom creation message sent with message URI {}", deactivateAtomMessage.getMessageURI());
    }

    private WonMessage createWonMessage(WonNodeInformationService wonNodeInformationService, URI atomURI,
                    URI wonNodeURI) throws WonMessageBuilderException {
        return WonMessageBuilder
                        .deactivate()
                        .atom(atomURI)
                        .direction().fromOwner()
                        .build();
    }
}
