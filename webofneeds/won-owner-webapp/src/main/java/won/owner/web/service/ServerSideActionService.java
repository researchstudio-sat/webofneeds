package won.owner.web.service;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import won.owner.pojo.ServerSideConnectPayload;
import won.owner.service.impl.OwnerApplicationService;
import won.owner.web.service.serversideaction.EventTriggeredAction;
import won.owner.web.service.serversideaction.EventTriggeredActionContainer;
import won.protocol.exception.WonMessageProcessingException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.AuthenticationThreadLocal;
import won.protocol.util.linkeddata.LinkedDataSource;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Component
public class ServerSideActionService implements WonMessageProcessor {
    EventTriggeredActionContainer<WonMessage> actionContainer = new EventTriggeredActionContainer<WonMessage>();
    @Autowired
    WonNodeInformationService wonNodeInformationService;
    @Autowired
    private LinkedDataSource linkedDataSourceOnBehalfOfAtom;
    @Autowired
    private LinkedDataSource linkedDataSource;
    // ownerApplicationService for sending messages from the owner to the node
    private OwnerApplicationService ownerApplicationService;

    /**
     * Connect the specified sockets, optionally waiting until the respective atoms
     * have been created. Use the specified authentication for sending messages on
     * behalf of the user the authentication was obtained from.
     */
    public void connect(ServerSideConnectPayload connectAction, Authentication authentication) {
        // a little bit of shared state for the actions we are creating:
        if (StringUtils.isEmpty(connectAction.getFromSocket()) || StringUtils.isEmpty(connectAction.getToSocket())) {
            throw new IllegalArgumentException("From and To Socket expected");
        }
        // we want to have 2 atoms before we can start connecting:
        final AtomicInteger atomCounter = new AtomicInteger(0);
        final AtomicBoolean connectSent = new AtomicBoolean(false);
        final URI fromSocket = URI.create(connectAction.getFromSocket());
        final String message = connectAction.getMessage();
        final URI toSocket = URI.create(connectAction.getToSocket());
        // count the sockets that are NOT pending (= the number of atoms that have already been created)
        int atoms = 2;
        if (connectAction.isFromPending()) {
            atoms--;
        }
        if (connectAction.isToPending()) {
            atoms--;
        }
        atomCounter.set(atoms);
        final Function<Optional<WonMessage>, Collection<EventTriggeredAction<WonMessage>>> action = new Function<Optional<WonMessage>, Collection<EventTriggeredAction<WonMessage>>>() {
            @Override
            public Collection<EventTriggeredAction<WonMessage>> apply(Optional<WonMessage> msg) {
                // process success responses for create
                if (msg.isPresent() && atomCounter.get() < 2 && isResponseToCreateOfSockets(msg.get(), connectAction)) {
                    // we're processing a response event for the creation of one of our atoms.
                    atomCounter.incrementAndGet();
                }
                // maybe we're processing a response to create, maybe we're processing an empty
                // event:
                if (atomCounter.get() >= 2) {
                    // we have all the atoms we need for the connect.
                    if (connectSent.compareAndSet(false, true)) {
                        // we haven't sent the connect yet, do it now and register a new action
                        // waiting for the connect on the receiving end.
                        sendConnect(fromSocket, toSocket, message, authentication);
                        return Arrays.asList(new EventTriggeredAction<WonMessage>(
                                        String.format("Connect %s and %s: Expecting incoming connect from %s for %s",
                                                        fromSocket, toSocket, fromSocket, toSocket),
                                        m -> isConnectFromSocketForSocket(m.get(), fromSocket, toSocket), this));
                    } else {
                        // we have sent the connect, check if we're processing the connect on the
                        // receiving end. If so, send an open.
                        if (connectAction.isAutoOpen()
                                        && isConnectFromSocketForSocket(msg.get(), fromSocket, toSocket)) {
                            reactWithConnect(msg.get(), message, authentication);
                            // that's it - don't register any more actions
                        }
                    }
                } else {
                    // we are still waiting for atom creation to finish. return this action waiting
                    // for another create response
                    return Arrays.asList(new EventTriggeredAction<>(
                                    String.format("Connect %s and %s: Expecting response for create", fromSocket,
                                                    toSocket),
                                    m -> m.isPresent() && isResponseToCreateOfSockets(m.get(), connectAction), this));
                }
                // none of the above - this is the last execution of this action.
                return Collections.emptyList();
            }
        };
        // add the action in such a way that it is triggered on add (with an empty
        // Optional<WonMessage>)
        this.actionContainer.addAction(new EventTriggeredAction<WonMessage>(
                        String.format("Connect %s and %s", fromSocket, toSocket), action));
    }

    private boolean isResponseToCreateOfSockets(WonMessage msg, ServerSideConnectPayload connectAction) {
        return isResponseToCreateOfSocket(msg, connectAction.getFromSocket())
                        || isResponseToCreateOfSocket(msg, connectAction.getToSocket());
    }

    private boolean isResponseToCreateOfSocket(WonMessage msg, String socketUri) {
        if (!msg.isMessageWithResponse()) {
            return false;
        }
        WonMessage response = msg.getResponse().get();
        return response.getRespondingToMessageType() == WonMessageType.CREATE_ATOM
                        && response.getEnvelopeType() == WonMessageDirection.FROM_SYSTEM
                        && socketUri.startsWith(msg.getAtomURI() + "#");
    }

    private boolean isConnectFromSocketForSocket(WonMessage msg, URI sender, URI receiver) {
        if (!msg.isMessageWithBothResponses()) {
            // we expect a message with responses from both nodes
            return false;
        }
        return msg.getMessageType() == WonMessageType.CONNECT
                        && msg.getSenderSocketURI() != null && sender.equals(msg.getSenderSocketURI())
                        && msg.getRecipientSocketURI() != null && receiver.equals(msg.getRecipientSocketURI());
    }

    private void sendConnect(URI fromSocket, URI toSocket, String message, Authentication authentication) {
        WonMessage msgToSend = WonMessageBuilder
                        .connect()
                        .sockets().sender(fromSocket).recipient(toSocket)
                        .content().text(message)
                        .build();
        try {
            AuthenticationThreadLocal.setAuthentication(authentication);
            ownerApplicationService.prepareAndSendMessage(msgToSend);
        } finally {
            // be sure to remove the principal from the threadlocal
            AuthenticationThreadLocal.remove();
        }
    }

    private void reactWithConnect(WonMessage connectMessageToReactTo, String message, Authentication authentication) {
        WonMessage msgToSend = WonMessageBuilder
                        .connect()
                        .sockets().reactingTo(connectMessageToReactTo)
                        .content().text(message)
                        .build();
        try {
            AuthenticationThreadLocal.setAuthentication(authentication);
            ownerApplicationService.prepareAndSendMessage(msgToSend);
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

    public void setLinkedDataSourceOnBehalfOfAtom(LinkedDataSource linkedDataSourceOnBehalfOfAtom) {
        this.linkedDataSourceOnBehalfOfAtom = linkedDataSourceOnBehalfOfAtom;
    }
}
