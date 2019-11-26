package won.node.service.persistence;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import won.protocol.exception.DuplicateResponseException;
import won.protocol.exception.IncoherentDatabaseStateException;
import won.protocol.exception.NoSuchMessageException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.WonMessageType;
import won.protocol.message.WonMessageUtils;
import won.protocol.model.AtomMessageContainer;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionMessageContainer;
import won.protocol.model.DatasetHolder;
import won.protocol.model.MessageContainer;
import won.protocol.model.MessageEvent;
import won.protocol.model.PendingConfirmation;
import won.protocol.repository.AtomMessageContainerRepository;
import won.protocol.repository.ConnectionContainerRepository;
import won.protocol.repository.ConnectionMessageContainerRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.MessageContainerRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.PendingConfirmationRepository;

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
    private PendingConfirmationRepository pendingConfirmationRepository;

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
        Set<PendingConfirmation> pending = pendingConfirmationRepository
                        .findAllByMessageContainerIdAndConfirmingMessageURIIn(container.getId(), previous);
        sw.stop();
        sw.start("determine confirmed");
        Set<URI> confirmed = pending.stream()
                        .flatMap(pc -> pc.getConfirmedMessageURIs().stream())
                        .collect(Collectors.toSet());
        sw.stop();
        if (logger.isDebugEnabled()) {
            logger.debug("{} unconfirmed for message container of {}, Removing {} ",
                            new Object[] { container.getUnconfirmed().size(), parent, confirmed.size() });
            logger.debug("unconfirmed: {}", container.getUnconfirmed());
            logger.debug("transitively confirmed: {}", confirmed);
        }
        sw.start("remove unconfirmed");
        container.removeUnconfirmed(confirmed);
        sw.stop();
        sw.start("remove pending");
        // container.removePendingConfirmations(previous);
        pendingConfirmationRepository.deleteByMessageContainerIdAndConfirmingMessageURIIn(container.getId(), previous);
        sw.stop();
        if (logger.isDebugEnabled()) {
            logger.debug("{} messages left in unconfirmed list of the message container of {}",
                            new Object[] { container.getUnconfirmed().size(), parent });
        }
        logger.debug("removing confirmed took {} millis", sw.getLastTaskTimeMillis());
        if (logger.isDebugEnabled()) {
            logger.debug("Timinig info: \n{}", sw.prettyPrint());
        }
    }

    public void saveMessage(final WonMessage messageOrDeliveryChain, URI parent) {
        for (WonMessage wonMessage : messageOrDeliveryChain.getAllMessages()) {
            logger.debug("STORING {} message {} under parent {}", new Object[] { wonMessage.getMessageType(),
                            wonMessage.getMessageURI(), parent });
            MessageContainer container = loadOrCreateMessageContainer(parent, wonMessage.getMessageType());
            // unconfirmed list:
            // - add any success response message from partner in a connection
            // - add any of our system responses messages if we 're in an atom's message
            // container.
            if (wonMessage.getMessageTypeRequired().isSuccessResponse()) {
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
            }
            if (isExternalSuccessResponseInConnection(parent, wonMessage)) {
                removeConfirmed(container, wonMessage, parent);
                if (logger.isDebugEnabled()) {
                    logger.debug("Adding as unconfirmed message: {}", wonMessage.getMessageURIRequired());
                }
                container.addUnconfirmed(wonMessage.getMessageURIRequired());
            } else if (isOwnSuccessResponseInConnection(parent, wonMessage)) {
                // our message confirms a number of remote ones (those that were in the
                // confirmed list). However, only when a remote message confirms ours, we can
                // remove them from the unconfirmed list
                List<URI> previous = wonMessage.getPreviousMessageURIs();
                if (!previous.isEmpty()) {
                    PendingConfirmation newPending = new PendingConfirmation(container,
                                    wonMessage.getMessageURIRequired(),
                                    previous.stream().collect(Collectors.toSet()));
                    pendingConfirmationRepository.save(newPending);
                }
            } else if (isOwnSuccessResponseInAtom(parent, wonMessage)) {
                removeConfirmed(container, wonMessage, parent);
                if (logger.isDebugEnabled()) {
                    logger.debug("Adding as unconfirmed message: {}", wonMessage.getMessageURIRequired());
                }
                container.addUnconfirmed(wonMessage.getMessageURI());
            }
            MessageEvent event = new MessageEvent(parent, wonMessage, container);
            // a message can be in multiple containers (=parents), such messages share a
            // datasetholder
            Optional<DatasetHolder> datasetHolder = datasetHolderRepository.findOneByUri(wonMessage.getMessageURI());
            event.setDatasetHolder(datasetHolder.orElseGet(() -> new DatasetHolder(wonMessage.getMessageURI(),
                            WonMessageEncoder.encodeAsDataset(wonMessage))));
            messageContainerRepository.save(container);
            messageEventRepository.save(event);
        }
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
            MessageContainer container = atomMessageContainerRepository.findOneByParentUriForUpdate(parent);
            if (container != null)
                return container;
            AtomMessageContainer nec = new AtomMessageContainer(null, parent);
            atomMessageContainerRepository.saveAndFlush(nec);
            return atomMessageContainerRepository.findOne(nec.getId());
        } else if (WonMessageType.CONNECT.equals(messageType)
                        || WonMessageType.SOCKET_HINT_MESSAGE.equals(messageType)) {
            // create a connection event container witn null parent (because it will only be
            // persisted at a later point in
            // time)
            MessageContainer container = connectionMessageContainerRepository.findOneByParentUriForUpdate(parent);
            if (container != null)
                return container;
            ConnectionMessageContainer cec = new ConnectionMessageContainer(null, parent);
            connectionMessageContainerRepository.save(cec);
            return connectionMessageContainerRepository.findOne(cec.getId());
        }
        return messageContainerRepository.findOneByParentUriForUpdate(parent)
                        .orElseThrow(() -> new IncoherentDatabaseStateException(
                                        "Cannot store '" + messageType + "' event: unable to find "
                                                        + "event container with parent URI '" + parent + "'"));
    }
}
