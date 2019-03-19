package won.owner.web.service;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import won.owner.pojo.FacetToConnect;
import won.owner.service.impl.OwnerApplicationService;
import won.owner.web.service.serversideaction.EventTriggeredAction;
import won.owner.web.service.serversideaction.EventTriggeredActionContainer;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.AuthenticationThreadLocal;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

@Component
public class ServerSideActionService implements WonMessageProcessor {

    EventTriggeredActionContainer<WonMessage> actionContainer = new EventTriggeredActionContainer<WonMessage>();

    @Autowired
    WonNodeInformationService wonNodeInformationService;

    @Autowired
    private LinkedDataSource linkedDataSourceOnBehalfOfNeed;

    @Autowired
    private LinkedDataSource linkedDataSource;

    // ownerApplicationService for sending messages from the owner to the node
    private OwnerApplicationService ownerApplicationService;

    /**
     * Connect the specified facets, optionally waiting until the respective needs have been created. Use the specified
     * authentication for sending messages on behalf of the user the authentication was obtained from.
     */
    public void connect(List<FacetToConnect> facets, Authentication authentication) {
        // a little bit of shared state for the actions we are creating:
        if (facets == null || facets.size() != 2) {
            throw new IllegalArgumentException("2 facets expected");
        }
        // we want to have 2 needs before we can start connecting:
        final AtomicInteger needCounter = new AtomicInteger(0);
        final AtomicBoolean connectSent = new AtomicBoolean(false);
        final URI fromFacet = URI.create(facets.get(0).getFacet());
        final URI toFacet = URI.create(facets.get(1).getFacet());

        // count the pending facets
        int needs = (int) facets.stream().filter(f -> !f.isPending()).count();
        needCounter.set(needs);

        final Function<Optional<WonMessage>, Collection<EventTriggeredAction<WonMessage>>> action = new Function<Optional<WonMessage>, Collection<EventTriggeredAction<WonMessage>>>() {
            @Override
            public Collection<EventTriggeredAction<WonMessage>> apply(Optional<WonMessage> msg) {
                // process success responses for create
                if (msg.isPresent() && needCounter.get() < 2 && isResponseToCreateOfFacets(msg.get(), facets)
                        && isSuccessResponse(msg.get())) {
                    // we're processing a response event for the creation of one of our needs.
                    needCounter.incrementAndGet();
                }
                // maybe we're processing a response to create, maybe we're processing an empty event:
                if (needCounter.get() >= 2) {
                    // we have all the needs we need for the connect.
                    if (connectSent.compareAndSet(false, true)) {
                        // we haven't sent the connect yet, do it now and register a new action
                        // waiting for the connect on the receiving end.
                        sendConnect(fromFacet, toFacet, authentication);
                        return Arrays.asList(new EventTriggeredAction<WonMessage>(
                                String.format("Connect %s and %s: Expecting incoming connect from %s for %s", fromFacet,
                                        toFacet, fromFacet, toFacet),
                                m -> isConnectFromFacetForFacet(m.get(), fromFacet, toFacet), this));
                    } else {
                        // we have sent the connect, check if we're processing the connect on the
                        // receiving end. If so, send an open.
                        if (isConnectFromFacetForFacet(msg.get(), fromFacet, toFacet)) {
                            sendOpen(msg.get(), authentication);
                            // that's it - don't register any more actions
                        }
                    }
                } else {
                    // we are still waiting for need creation to finish. return this action waiting for another create
                    // response
                    return Arrays.asList(new EventTriggeredAction<>(
                            String.format("Connect %s and %s: Expecting response for create", fromFacet, toFacet),
                            m -> m.isPresent() && isResponseToCreateOfFacets(m.get(), facets), this));

                }
                // none of the above - this is the last execution of this action.
                return Collections.emptyList();
            }
        };

        // add the action in such a way that it is triggered on add (with an empty Optional<WonMessage>)
        this.actionContainer.addAction(
                new EventTriggeredAction<WonMessage>(String.format("Connect %s and %s", fromFacet, toFacet), action));

    }

    private boolean isSuccessResponse(WonMessage msg) {
        return msg.getMessageType() == WonMessageType.SUCCESS_RESPONSE;
    }

    private boolean isResponseToCreateOfFacets(WonMessage msg, List<FacetToConnect> facets) {
        return facets.stream().anyMatch(facet -> facet.isPending() && isResponseToCreateOfFacet(msg, facet));
    }

    private boolean isResponseToCreateOfFacet(WonMessage msg, FacetToConnect facet) {
        return msg.getIsResponseToMessageType() == WonMessageType.CREATE_NEED
                && msg.getEnvelopeType() == WonMessageDirection.FROM_SYSTEM
                && facet.getFacet().startsWith(msg.getReceiverNeedURI().toString() + "#");
    }

    private boolean isConnectFromFacetForFacet(WonMessage msg, URI sender, URI receiver) {
        return msg.getMessageType() == WonMessageType.CONNECT
                && msg.getEnvelopeType() == WonMessageDirection.FROM_EXTERNAL && msg.getSenderFacetURI() != null
                && sender.equals(msg.getSenderFacetURI()) && msg.getReceiverFacetURI() != null
                && receiver.equals(msg.getReceiverFacetURI());
    }

    private void sendConnect(URI fromFacet, URI toFacet, Authentication authentication) {
        URI fromNeedURI = URI.create(fromFacet.toString().replaceFirst("#.+$", ""));
        URI toNeedURI = URI.create(toFacet.toString().replaceFirst("#.+$", ""));
        URI fromWonNodeURI = WonLinkedDataUtils.getWonNodeURIForNeedOrConnectionURI(fromNeedURI, linkedDataSource);
        URI toWonNodeURI = WonLinkedDataUtils.getWonNodeURIForNeedOrConnectionURI(toNeedURI, linkedDataSource);
        URI messageURI = wonNodeInformationService.generateEventURI(fromWonNodeURI);
        WonMessage msgToSend = WonMessageBuilder.setMessagePropertiesForConnect(messageURI, Optional.of(fromFacet),
                fromNeedURI, fromWonNodeURI, Optional.of(toFacet), toNeedURI, toWonNodeURI,
                "Connect message automatically sent by a server-side action").build();
        try {
            AuthenticationThreadLocal.setAuthentication(authentication);
            ownerApplicationService.sendWonMessage(msgToSend);
        } finally {
            // be sure to remove the principal from the threadlocal
            AuthenticationThreadLocal.remove();
        }
    }

    private void sendOpen(WonMessage connectMessageToReactTo, Authentication authentication) {
        URI fromWonNodeURI = connectMessageToReactTo.getReceiverNodeURI();
        URI messageURI = wonNodeInformationService.generateEventURI(fromWonNodeURI);
        WonMessage msgToSend = WonMessageBuilder.setMessagePropertiesForOpen(messageURI, connectMessageToReactTo,
                "Open message automatically sent by a server-side action").build();
        try {
            AuthenticationThreadLocal.setAuthentication(authentication);
            ownerApplicationService.sendWonMessage(msgToSend);
        } finally {
            // be sure to remove the principal from the threadlocal
            AuthenticationThreadLocal.remove();
        }
    }

    @Override
    public WonMessage process(WonMessage message) throws WonMessageProcessingException {
        actionContainer.executeFor(Optional.of(message));
        return message;
    }

    public void setOwnerApplicationService(OwnerApplicationService ownerApplicationService) {
        this.ownerApplicationService = ownerApplicationService;
    }

    public void setLinkedDataSourceOnBehalfOfNeed(LinkedDataSource linkedDataSourceOnBehalfOfNeed) {
        this.linkedDataSourceOnBehalfOfNeed = linkedDataSourceOnBehalfOfNeed;
    }

}
