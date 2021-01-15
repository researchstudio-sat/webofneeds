package won.node.service.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import won.protocol.exception.DuplicateResponseException;
import won.protocol.exception.IncoherentDatabaseStateException;
import won.protocol.exception.NoSuchMessageException;
import won.protocol.message.*;
import won.protocol.model.*;
import won.protocol.repository.*;

import javax.persistence.EntityManager;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    protected ConnectionContainerRepository connectionContainerRepository;
    @Autowired
    protected ConnectionRepository connectionRepository;
    @Autowired
    protected AtomMessageContainerRepository atomMessageContainerRepository;
    @Autowired
    protected MessageContainerRepository messageContainerRepository;
    @Autowired
    protected ConnectionMessageContainerRepository connectionMessageContainerRepository;
    @Autowired
    protected DatasetHolderRepository datasetHolderRepository;
    @Autowired
    private MessageEventRepository messageEventRepository;
    @Autowired
    private EntityManager entityManager;

    public Optional<MessageEvent> getMessage(URI messageURI, URI parentURI) {
        return messageEventRepository.findOneByMessageURIAndParentURI(messageURI, parentURI);
    }

    public MessageEvent getMessageRequired(URI messageURI, URI parentURI) {
        return getMessage(messageURI, parentURI).orElseThrow(() -> new NoSuchMessageException(messageURI));
    }

    public Optional<URI> getParentofMessage(WonMessage msg, WonMessageDirection direction) {
        WonMessageType type = msg.getMessageTypeRequired();
        Optional<URI> connectionFromResponse = Optional.empty();
        if (type.isResponseMessage()) {
            type = msg.getRespondingToMessageTypeRequired();
            // if we are handling a response from a socket, the connection is there:
            connectionFromResponse = Optional.ofNullable(msg.getConnectionURI());
        }
        if (type.isAtomSpecificMessage()) {
            // no need to look into the db:
            return WonMessageUtils.getParentAtomUri(msg, direction);
        } else if (type.isConnectionSpecificMessage() && !type.isHintMessage()) {
            Optional<URI> ourSocket = Optional.empty();
            Optional<URI> theirSocket = Optional.empty();
            if (direction.isFromExternal()) {
                ourSocket = Optional.ofNullable(msg.getRecipientSocketURIRequired());
                theirSocket = Optional.ofNullable(msg.getSenderSocketURIRequired());
            } else {
                ourSocket = Optional.ofNullable(msg.getSenderSocketURIRequired());
                theirSocket = Optional.ofNullable(msg.getRecipientSocketURIRequired());
            }
            if (ourSocket.isPresent() && theirSocket.isPresent()) {
                Optional<Connection> con = Optional.empty();
                if (Objects.equals(ourSocket.get(), theirSocket.get()) && connectionFromResponse.isPresent()) {
                    con = connectionRepository.findOneByConnectionURI(connectionFromResponse.get());
                } else {
                    con = connectionRepository.findOneBySocketURIAndTargetSocketURI(
                                    ourSocket.get(),
                                    theirSocket.get());
                }
                return con.map(Connection::getConnectionURI);
            }
        }
        return Optional.empty();
    }

    public Optional<URI> getAtomOfMessage(WonMessage message, WonMessageDirection direction) {
        return WonMessageUtils.getParentAtomUri(message, direction);
    }

    public Optional<URI> getConnectionofMessage(WonMessage msg, WonMessageDirection direction) {
        WonMessageType type = msg.getMessageTypeRequired();
        if (type.isResponseMessage()) {
            type = msg.getRespondingToMessageTypeRequired();
        }
        if (!type.isConnectionSpecificMessage()) {
            return Optional.empty();
        }
        Optional<URI> ourSocket = Optional.empty();
        Optional<URI> theirSocket = Optional.empty();
        if (type.isSocketHintMessage()) {
            // special handling for hints
            ourSocket = Optional.ofNullable(msg.getRecipientSocketURIRequired());
            theirSocket = Optional.ofNullable(msg.getHintTargetSocketURIRequired());
        } else {
            if (direction.isFromExternal()) {
                ourSocket = Optional.ofNullable(msg.getRecipientSocketURIRequired());
                theirSocket = Optional.ofNullable(msg.getSenderSocketURIRequired());
            } else {
                ourSocket = Optional.ofNullable(msg.getSenderSocketURIRequired());
                theirSocket = Optional.ofNullable(msg.getRecipientSocketURIRequired());
            }
        }
        if (ourSocket.isPresent() && theirSocket.isPresent()) {
            return connectionRepository.findOneBySocketURIAndTargetSocketURI(ourSocket.get(),
                            theirSocket.get()).map(Connection::getConnectionURI);
        }
        return Optional.empty();
    }

    /**
     * Removes the URIs that are confirmed by the specified message from the
     * parent's unconfirmed list.
     * 
     * @param container
     * @param message
     * @param parent
     */
    private void removeConfirmed(final MessageContainer container, final WonMessage message, final URI parent) {
        // the message might be an external message referencing some of our messages.
        // if that is the case, that transitively confirms their earlier messages
        StopWatch sw = new StopWatch();
        if (logger.isDebugEnabled()) {
            logger.debug("Checking if message {} confirms any unconfirmed messages in the message container of {}",
                            message.toShortStringForDebug(), parent);
        }
        sw.start("get previous URIs from message");
        Set<URI> previous = message.getPreviousMessageURIs().stream().collect(Collectors.toSet());
        sw.stop();
        if (logger.isDebugEnabled()) {
            logger.debug("{} previous messages referenced by external response {} ",
                            previous.size(), message.toShortStringForDebug());
            logger.debug("previous messages: {}", previous);
        }
        if (previous.isEmpty()) {
            logger.debug("no previous messages found, not removing any unconfirmed messages");
            return;
        }
        sw.start("load pending");
        Map<URI, Set<URI>> pending = container.getPendingConfirmations();
        sw.stop();
        sw.start("determine confirmed");
        Set<URI> confirmed = pending
                        .entrySet()
                        .stream()
                        .filter(e -> previous.contains(e.getKey()))
                        .flatMap(e -> e.getValue().stream())
                        .collect(Collectors.toSet());
        sw.stop();
        if (logger.isDebugEnabled()) {
            logger.debug("{} unconfirmed for message container of {}, removing {} transitively confirmed",
                            new Object[] { container.getUnconfirmedCount(), parent, confirmed.size() });
            logger.debug("unconfirmed: {}", container.peekAtUnconfirmed());
            logger.debug("transitively confirmed: {}", confirmed);
        }
        sw.start("remove unconfirmed");
        container.removeUnconfirmed(confirmed);
        sw.stop();
        sw.start("remove pending");
        // container.removePendingConfirmations(previous);
        container.removePendingConfirmations(previous);
        sw.stop();
        if (logger.isDebugEnabled()) {
            logger.debug("{} messages left in unconfirmed list of the message container of {}",
                            new Object[] { container.getUnconfirmedCount(), parent });
        }
        logger.debug("removing confirmed took {} millis", sw.getLastTaskTimeMillis());
        if (logger.isDebugEnabled()) {
            logger.debug("Timinig info: \n{}", sw.prettyPrint());
        }
    }

    public void saveMessage(final WonMessage messageOrDeliveryChain, URI parent) {
        StopWatch sw = new StopWatch();
        for (WonMessage wonMessage : messageOrDeliveryChain.getAllMessages()) {
            logger.debug("STORING {} message {} under parent {}", new Object[] { wonMessage.getMessageType(),
                            wonMessage.getMessageURI(), parent });
            sw.start("get message container");
            MessageContainer container = loadOrCreateMessageContainer(parent, wonMessage.getMessageType());
            sw.stop();
            // unconfirmed list:
            // - add any success response message from partner in a connection
            // - add any of our system responses messages if we 're in an atom's message
            // container.
            if (wonMessage.getMessageTypeRequired().isSuccessResponse()) {
                sw.start("check for duplicate response");
                // make sure it's not a duplicate response
                URI respondingTo = wonMessage.getRespondingToMessageURIRequired();
                URI responseContainer = wonMessage.getAtomURI();
                if (responseContainer == null) {
                    responseContainer = wonMessage.getConnectionURIRequired();
                }
                Optional<MessageEvent> duplicate = messageEventRepository
                                .findOneByParentURIAndRespondingToURIAndResponseContainerURI(parent, respondingTo,
                                                responseContainer);
                if (duplicate.isPresent()) {
                    logger.debug("Detected duplicate response to {} from container {} in container {}: {}",
                                    new Object[] { respondingTo, responseContainer, parent,
                                                    wonMessage.toShortStringForDebug() });
                    throw new DuplicateResponseException(MessageFormat.format(
                                    "Detected duplicate response to {0} from container {1} in container {2}: {3}",
                                    respondingTo, responseContainer, parent, wonMessage.toShortStringForDebug()));
                }
                sw.stop();
            }
            if (isExternalSuccessResponseInConnection(parent, wonMessage)) {
                sw.start("process external response in connection");
                if (logger.isDebugEnabled()) {
                    logger.debug("In connection, processing external response {} to {} in container {}", new Object[] {
                                    wonMessage.toShortStringForDebug(), wonMessage.getRespondingToMessageURI(),
                                    parent });
                }
                removeConfirmed(container, wonMessage, parent);
                if (logger.isDebugEnabled()) {
                    logger.debug("Adding as unconfirmed message: {} in container {}",
                                    wonMessage.getMessageURIRequired(), parent);
                }
                addUnconfirmed(container, wonMessage);
                sw.stop();
            } else if (isOwnSuccessResponseInConnection(parent, wonMessage)) {
                sw.start("process own response in connection's message container");
                if (logger.isDebugEnabled()) {
                    logger.debug("In connection, processing own response {} to {} in container {}", new Object[] {
                                    wonMessage.toShortStringForDebug(), wonMessage.getRespondingToMessageURI(),
                                    parent });
                }
                // our message confirms a number of remote ones (those that were in the
                // unconfirmed list). However, only when a remote message confirms ours, we can
                // remove them from the unconfirmed list
                List<URI> previous = wonMessage.getPreviousMessageURIs();
                if (!previous.isEmpty()) {
                    container.addPendingConfirmation(
                                    wonMessage.getMessageURIRequired(),
                                    previous.stream().collect(Collectors.toSet()));
                }
                sw.stop();
            } else if (isOwnSuccessResponseInAtom(parent, wonMessage)) {
                sw.start("process own response in atom's message container");
                removeConfirmed(container, wonMessage, parent);
                if (logger.isDebugEnabled()) {
                    logger.debug("Adding as unconfirmed message: {} in container {}",
                                    wonMessage.getMessageURIRequired(), parent);
                }
                addUnconfirmed(container, wonMessage);
                sw.stop();
            }
            sw.start("create event");
            MessageEvent event = new MessageEvent(parent, wonMessage, container);
            // a message can be in multiple containers (=parents), such messages share a
            // datasetholder
            sw.stop();
            sw.start("get event dataset id (if any)");
            Optional<Long> datasetHolderId = datasetHolderRepository.findIdByUri(wonMessage.getMessageURIRequired());
            sw.stop();
            sw.start("Add dataset to message");
            if (datasetHolderId.isPresent()) {
                DatasetHolder datasetHolder = entityManager.getReference(DatasetHolder.class, datasetHolderId.get());
                event.setDatasetHolder(datasetHolder);
            } else {
                event.setDatasetHolder(new DatasetHolder(wonMessage.getMessageURI(),
                                WonMessageEncoder.encodeAsDataset(wonMessage)));
            }
            sw.stop();
            sw.start("store message");
            event = messageEventRepository.save(event);
            sw.stop();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Timing info:\n{}", sw.prettyPrint());
        }
    }

    private void addUnconfirmed(MessageContainer container, WonMessage toAdd) {
        container.addUnconfirmed(toAdd.getMessageURIRequired());
    }

    public boolean isOwnSuccessResponseInAtom(URI parent, WonMessage wonMessage) {
        return wonMessage.getAtomURI() != null && Objects.equals(parent, wonMessage.getAtomURI())
                        && wonMessage.getEnvelopeType().isFromSystem()
                        && wonMessage.getMessageTypeRequired().isSuccessResponse()
                        && wonMessage.getRespondingToMessageTypeRequired().isAtomSpecificMessage();
    }

    public boolean isExternalSuccessResponseInConnection(URI parent, WonMessage wonMessage) {
        return wonMessage.getConnectionURI() != null
                        && !Objects.equals(parent, wonMessage.getConnectionURI())
                        && wonMessage.getEnvelopeType().isFromSystem()
                        && wonMessage.getMessageTypeRequired().isSuccessResponse()
                        && wonMessage.getRespondingToMessageTypeRequired().isConnectionSpecificMessage();
    }

    public boolean isOwnSuccessResponseInConnection(URI parent, WonMessage wonMessage) {
        return wonMessage.getConnectionURI() != null
                        && Objects.equals(parent, wonMessage.getConnectionURI())
                        && wonMessage.getEnvelopeType().isFromSystem()
                        && wonMessage.getMessageTypeRequired().isSuccessResponse()
                        && wonMessage.getRespondingToMessageTypeRequired().isConnectionSpecificMessage();
    }

    public MessageContainer loadOrCreateMessageContainer(final URI parent, final WonMessageType messageType) {
        if (WonMessageType.CREATE_ATOM.equals(messageType)) {
            // create an atom event container with null parent (because it will only be
            // persisted at a later point in time)
            MessageContainer container = atomMessageContainerRepository.findOneByParentUri(parent);
            if (container != null) {
                return container;
            }
            AtomMessageContainer nec = new AtomMessageContainer(null, parent);
            nec = atomMessageContainerRepository.save(nec);
            return atomMessageContainerRepository.findById(nec.getId()).get();
        } else if (WonMessageType.CONNECT.equals(messageType)
                        || WonMessageType.SOCKET_HINT_MESSAGE.equals(messageType)) {
            // create a connection event container witn null parent (because it will only be
            // persisted at a later point in
            // time)
            MessageContainer container = connectionMessageContainerRepository.findOneByParentUri(parent);
            if (container != null) {
                return container;
            }
            ConnectionMessageContainer cec = new ConnectionMessageContainer(null, parent);
            cec = connectionMessageContainerRepository.save(cec);
            return connectionMessageContainerRepository.findById(cec.getId()).get();
        }
        Optional<MessageContainer> mc = messageContainerRepository.findOneByParentUri(parent);
        return mc.orElseThrow(() -> new IncoherentDatabaseStateException(
                        "Cannot store '" + messageType + "' event: unable to find "
                                        + "event container with parent URI '" + parent + "'"));
    }
}
