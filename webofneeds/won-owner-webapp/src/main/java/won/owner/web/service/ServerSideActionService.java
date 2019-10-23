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

import won.owner.pojo.SocketToConnect;
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
    public void connect(List<SocketToConnect> sockets, Authentication authentication) {
        // a little bit of shared state for the actions we are creating:
        if (sockets == null || sockets.size() != 2) {
            throw new IllegalArgumentException("2 sockets expected");
        }
        // we want to have 2 atoms before we can start connecting:
        final AtomicInteger atomCounter = new AtomicInteger(0);
        final AtomicBoolean connectSent = new AtomicBoolean(false);
        final URI fromSocket = URI.create(sockets.get(0).getSocket());
        final URI toSocket = URI.create(sockets.get(1).getSocket());
        // count the pending sockets
        int atoms = (int) sockets.stream().filter(f -> !f.isPending()).count();
        atomCounter.set(atoms);
        final Function<Optional<WonMessage>, Collection<EventTriggeredAction<WonMessage>>> action = new Function<Optional<WonMessage>, Collection<EventTriggeredAction<WonMessage>>>() {
            @Override
            public Collection<EventTriggeredAction<WonMessage>> apply(Optional<WonMessage> msg) {
                // process success responses for create
                if (msg.isPresent() && atomCounter.get() < 2 && isResponseToCreateOfSockets(msg.get(), sockets)
                                && isSuccessResponse(msg.get())) {
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
                        sendConnect(fromSocket, toSocket, authentication);
                        return Arrays.asList(new EventTriggeredAction<WonMessage>(
                                        String.format("Connect %s and %s: Expecting incoming connect from %s for %s",
                                                        fromSocket, toSocket, fromSocket, toSocket),
                                        m -> isConnectFromSocketForSocket(m.get(), fromSocket, toSocket), this));
                    } else {
                        // we have sent the connect, check if we're processing the connect on the
                        // receiving end. If so, send an open.
                        if (isConnectFromSocketForSocket(msg.get(), fromSocket, toSocket)) {
                            sendOpen(msg.get(), authentication);
                            // that's it - don't register any more actions
                        }
                    }
                } else {
                    // we are still waiting for atom creation to finish. return this action waiting
                    // for another create response
                    return Arrays.asList(new EventTriggeredAction<>(
                                    String.format("Connect %s and %s: Expecting response for create", fromSocket,
                                                    toSocket),
                                    m -> m.isPresent() && isResponseToCreateOfSockets(m.get(), sockets), this));
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

    private boolean isSuccessResponse(WonMessage msg) {
        return msg.getMessageType() == WonMessageType.SUCCESS_RESPONSE;
    }

    private boolean isResponseToCreateOfSockets(WonMessage msg, List<SocketToConnect> sockets) {
        return sockets.stream().anyMatch(socket -> socket.isPending() && isResponseToCreateOfSocket(msg, socket));
    }

    private boolean isResponseToCreateOfSocket(WonMessage msg, SocketToConnect socket) {
        return msg.getIsResponseToMessageType() == WonMessageType.CREATE_ATOM
                        && msg.getEnvelopeType() == WonMessageDirection.FROM_SYSTEM
                        && socket.getSocket().startsWith(msg.getRecipientAtomURI().toString() + "#");
    }

    private boolean isConnectFromSocketForSocket(WonMessage msg, URI sender, URI receiver) {
        return msg.getMessageType() == WonMessageType.CONNECT
                        && msg.getEnvelopeType() == WonMessageDirection.FROM_EXTERNAL
                        && msg.getSenderSocketURI() != null && sender.equals(msg.getSenderSocketURI())
                        && msg.getRecipientSocketURI() != null && receiver.equals(msg.getRecipientSocketURI());
    }

    private void sendConnect(URI fromSocket, URI toSocket, Authentication authentication) {
        URI fromAtomURI = URI.create(fromSocket.toString().replaceFirst("#.+$", ""));
        URI toAtomURI = URI.create(toSocket.toString().replaceFirst("#.+$", ""));
        URI fromWonNodeURI = WonLinkedDataUtils.getWonNodeURIForAtomOrConnectionURI(fromAtomURI, linkedDataSource);
        URI toWonNodeURI = WonLinkedDataUtils.getWonNodeURIForAtomOrConnectionURI(toAtomURI, linkedDataSource);
        URI messageURI = wonNodeInformationService.generateEventURI(fromWonNodeURI);
        WonMessage msgToSend = WonMessageBuilder.setMessagePropertiesForConnect(messageURI, fromSocket,
                        fromAtomURI, fromWonNodeURI, toSocket, toAtomURI, toWonNodeURI,
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
        URI fromWonNodeURI = connectMessageToReactTo.getRecipientNodeURI();
        URI messageURI = wonNodeInformationService.generateEventURI(fromWonNodeURI);
        WonMessage msgToSend = WonMessageBuilder.setMessagePropertiesForConnect(messageURI, connectMessageToReactTo,
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

    public void setLinkedDataSourceOnBehalfOfAtom(LinkedDataSource linkedDataSourceOnBehalfOfAtom) {
        this.linkedDataSourceOnBehalfOfAtom = linkedDataSourceOnBehalfOfAtom;
    }
}
