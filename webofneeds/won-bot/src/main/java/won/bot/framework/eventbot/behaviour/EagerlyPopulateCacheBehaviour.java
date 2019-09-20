package won.bot.framework.eventbot.behaviour;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SuccessResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.protocol.message.WonMessage;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.CachingLinkedDataSource;
import won.protocol.util.linkeddata.LinkedDataSource;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class EagerlyPopulateCacheBehaviour extends BotBehaviour {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public EagerlyPopulateCacheBehaviour(EventListenerContext context) {
        super(context);
    }

    public EagerlyPopulateCacheBehaviour(EventListenerContext context, String name) {
        super(context, name);
    }

    @Override
    protected void onActivate(Optional<Object> message) {
        logger.debug("activating EagerlyPopulateCacheBehaviour");
        ProcessResponseAction processResponseAction = new ProcessResponseAction(context);
        ProcessIncomingMessageAction processIncomingMessageAction = new ProcessIncomingMessageAction(context);
        this.subscribeWithAutoCleanup(MessageFromOtherAtomEvent.class,
                        new ActionOnEventListener(context, processIncomingMessageAction));
        this.subscribeWithAutoCleanup(SuccessResponseEvent.class,
                        new ActionOnEventListener(context, processResponseAction));
        this.subscribeWithAutoCleanup(FailureResponseEvent.class,
                        new ActionOnEventListener(context, processResponseAction));
    }

    @Override
    protected void onCleanup() {
        logger.debug("deactivating EagerlyPopulateCacheBehaviour");
    }

    private class ProcessResponseAction extends BaseEventBotAction {
        public ProcessResponseAction(EventListenerContext context) {
            super(context);
        }

        @Override
        protected void doRun(Event event, EventListener executingListener) throws Exception {
            WonMessage responseWonMessage = null;
            if (event instanceof SuccessResponseEvent) {
                responseWonMessage = ((SuccessResponseEvent) event).getMessage();
            } else if (event instanceof FailureResponseEvent) {
                responseWonMessage = ((FailureResponseEvent) event).getMessage();
            } else {
                // can't process any other events
                return;
            }
            logger.debug("eagerly caching data in reaction to event {}", event);
            // put received message into cache
            LinkedDataSource linkedDataSource = context.getLinkedDataSource();
            if (linkedDataSource instanceof CachingLinkedDataSource) {
                URI requester = responseWonMessage.getRecipientAtomURI();
                ((CachingLinkedDataSource) linkedDataSource).addToCache(responseWonMessage.getCompleteDataset(),
                                responseWonMessage.getMessageURI(), requester);
                // load the original message(s) into cache, too
                Set<URI> toLoad = new HashSet<>();
                addIfNotNull(toLoad, responseWonMessage.getIsRemoteResponseToMessageURI());
                addIfNotNull(toLoad, responseWonMessage.getIsResponseToMessageURI());
                addIfNotNull(toLoad, responseWonMessage.getCorrespondingRemoteMessageURI());
                List<URI> previous = WonRdfUtils.MessageUtils.getPreviousMessageUrisIncludingRemote(responseWonMessage);
                addIfNotNull(toLoad, previous);
                toLoad.forEach(uri -> linkedDataSource.getDataForResource(uri, requester));
            }
        }
    }

    private class ProcessIncomingMessageAction extends BaseEventBotAction {
        public ProcessIncomingMessageAction(EventListenerContext context) {
            super(context);
        }

        @Override
        protected void doRun(Event event, EventListener executingListener) throws Exception {
            if (!(event instanceof MessageFromOtherAtomEvent)) {
                return;
            }
            logger.debug("eagerly caching data in reaction to event {}", event);
            MessageFromOtherAtomEvent msgEvent = (MessageFromOtherAtomEvent) event;
            WonMessage wonMessage = msgEvent.getWonMessage();
            LinkedDataSource linkedDataSource = context.getLinkedDataSource();
            if (linkedDataSource instanceof CachingLinkedDataSource) {
                ((CachingLinkedDataSource) linkedDataSource).addToCache(wonMessage.getCompleteDataset(),
                                wonMessage.getMessageURI(), wonMessage.getRecipientAtomURI());
                URI requester = wonMessage.getRecipientAtomURI();
                Set<URI> toLoad = new HashSet<>();
                addIfNotNull(toLoad, wonMessage.getCorrespondingRemoteMessageURI());
                List<URI> previous = WonRdfUtils.MessageUtils.getPreviousMessageUrisIncludingRemote(wonMessage);
                addIfNotNull(toLoad, previous);
                toLoad.forEach(uri -> linkedDataSource.getDataForResource(uri, requester));
            }
        }
    }

    private void addIfNotNull(Set<URI> uris, URI uri) {
        if (uri != null) {
            uris.add(uri);
        }
    }

    private void addIfNotNull(Set<URI> uris, List<URI> urisToAdd) {
        if (urisToAdd != null) {
            uris.addAll(urisToAdd);
        }
    }
}
