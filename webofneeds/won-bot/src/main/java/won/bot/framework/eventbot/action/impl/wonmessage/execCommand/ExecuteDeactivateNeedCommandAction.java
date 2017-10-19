package won.bot.framework.eventbot.action.impl.wonmessage.execCommand;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.deactivate.DeactivateNeedCommandEvent;
import won.bot.framework.eventbot.event.impl.command.deactivate.DeactivateNeedCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.deactivate.DeactivateNeedCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * Created by fsuda on 17.05.2017.
 */
public class ExecuteDeactivateNeedCommandAction extends BaseEventBotAction {

    public ExecuteDeactivateNeedCommandAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if (!(event instanceof DeactivateNeedCommandEvent)) return;
        DeactivateNeedCommandEvent deactivateNeedCommandEvent = (DeactivateNeedCommandEvent) event;

        EventListenerContext ctx = getEventListenerContext();
        EventBus bus = ctx.getEventBus();

        final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
        WonNodeInformationService wonNodeInformationService = ctx.getWonNodeInformationService();
        final URI needURI = wonNodeInformationService.generateNeedURI(wonNodeUri);
        WonMessage deactivateNeedMessage = createWonMessage(wonNodeInformationService, needURI, wonNodeUri);

        EventListener successCallback = new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                logger.debug("need creation successful, new need URI is {}", needURI);
                bus.publish(new DeactivateNeedCommandSuccessEvent(needURI,deactivateNeedCommandEvent));

            }
        };

        EventListener failureCallback = new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                String textMessage = WonRdfUtils.MessageUtils.getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                logger.debug("need creation failed for need URI {}, original message URI {}: {}", new Object[]{needURI, ((FailureResponseEvent) event).getOriginalMessageURI(), textMessage});
                bus.publish(new DeactivateNeedCommandFailureEvent(needURI, deactivateNeedCommandEvent, textMessage));
            }
        };
        EventBotActionUtils.makeAndSubscribeResponseListener(deactivateNeedMessage, successCallback, failureCallback, ctx);

        logger.debug("registered listeners for response to message URI {}", deactivateNeedMessage.getMessageURI());
        ctx.getWonMessageSender().sendWonMessage(deactivateNeedMessage);
        logger.debug("need creation message sent with message URI {}", deactivateNeedMessage.getMessageURI());
    }

    private WonMessage createWonMessage(WonNodeInformationService wonNodeInformationService, URI needURI, URI wonNodeURI) throws WonMessageBuilderException {
        return WonMessageBuilder
                .setMessagePropertiesForDeactivateFromOwner(
                        wonNodeInformationService.generateEventURI(wonNodeURI),
                        needURI,
                        wonNodeURI)
                .build();
    }
}
