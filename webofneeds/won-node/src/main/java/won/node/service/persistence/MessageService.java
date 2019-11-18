package won.node.service.persistence;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
import won.protocol.repository.AtomMessageContainerRepository;
import won.protocol.repository.ConnectionContainerRepository;
import won.protocol.repository.ConnectionMessageContainerRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.MessageContainerRepository;
import won.protocol.repository.MessageEventRepository;

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

    private void removeConfirmed(final MessageContainer container, final WonMessage message, final URI parent) {
        // the message might be an external message referencing some of our messages.
        // if that is the case, that transitively confirms their earlier messages
        if (logger.isDebugEnabled()) {
            logger.debug("Checking if message {} confirms any unconfirmed messages in the message container of {}",
                            message.toShortStringForDebug(), parent);
        }
        if (message.getMessageTypeRequired().isSuccessResponse()
                        && !Objects.equals(message.getConnectionURI(), parent)) {
            List<URI> previous = message.getPreviousMessageURIs();
            if (logger.isDebugEnabled()) {
                logger.debug("Message {} is an external response referencing {} messages",
                                message.toShortStringForDebug(), previous.size());
            }
            List<MessageEvent> prevMsgs = previous.stream().map(prev -> {
                Optional<MessageEvent> prevEvent = messageEventRepository.findOneByMessageURIAndParentURI(prev, parent);
                if (!prevEvent.isPresent()) {
                    // the message we received references a message we don't know.
                    // this can happen if the message overtook another one. We could keep it for a
                    // while
                    // and reprocess it after the next message in this container.
                    // for now, just cause a failure
                    throw new NoSuchMessageException(prev);
                }
                return prevEvent.get();
            }).collect(Collectors.toList());
            logger.debug("Successfully loaded the referenced messages");
            List<URI> confirmed = prevMsgs
                            .stream()
                            .flatMap(ev -> WonMessage.of(ev.getDatasetHolder().getDataset()).getPreviousMessageURIs()
                                            .stream())
                            .collect(Collectors.toList());
            if (logger.isDebugEnabled()) {
                logger.debug("Removing {} messages from the unconfirmed list of the message container of {} (size {})",
                                new Object[] { confirmed.size(), parent, container.getUnconfirmed().size() });
            }
            container.removeUnconfirmed(confirmed);
            if (logger.isDebugEnabled()) {
                logger.debug("Unconfirmed list of the message container of {} now contains {} messages",
                                new Object[] { confirmed.size(), parent, container.getUnconfirmed().size() });
            }
        }
    }

    public void saveMessage(final WonMessage messageOrDeliveryChain, URI parent) {
        for (WonMessage wonMessage : messageOrDeliveryChain.getAllMessages()) {
            logger.debug("STORING {} message {} under parent {}", new Object[] { wonMessage.getMessageType(),
                            wonMessage.getMessageURI(), parent });
            MessageContainer container = loadOrCreateMessageContainer(parent, wonMessage.getMessageType());
            removeConfirmed(container, wonMessage, parent);
            // unconfirmed list:
            // - add any success response message from partner in a connection
            // - add any of our system responses messages if we 're in an atom's message
            // container.
            if (wonMessage.getConnectionURI() != null && !Objects.equals(parent, wonMessage.getConnectionURI())
                            && wonMessage.getMessageTypeRequired().isSuccessResponse()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Adding as unconfirmed message: {}", wonMessage.getMessageURIRequired());
                }
                container.addUnconfirmed(wonMessage.getMessageURIRequired());
            } else if (wonMessage.getAtomURI() != null && Objects.equals(parent, wonMessage.getAtomURI())
                            && wonMessage.getEnvelopeType().isFromSystem()
                            && wonMessage.getMessageTypeRequired().isSuccessResponse()) {
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
            container.getEvents().add(event);
            messageEventRepository.save(event);
        }
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
