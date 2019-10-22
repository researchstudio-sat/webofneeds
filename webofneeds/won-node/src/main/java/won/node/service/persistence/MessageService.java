package won.node.service.persistence;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import won.protocol.exception.IncoherentDatabaseStateException;
import won.protocol.exception.NoSuchMessageException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.WonMessageType;
import won.protocol.model.AtomMessageContainer;
import won.protocol.model.ConnectionMessageContainer;
import won.protocol.model.DatasetHolder;
import won.protocol.model.MessageContainer;
import won.protocol.model.MessageEvent;
import won.protocol.model.MessageInContainer;
import won.protocol.repository.AtomMessageContainerRepository;
import won.protocol.repository.ConnectionContainerRepository;
import won.protocol.repository.ConnectionMessageContainerRepository;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.MessageContainerRepository;
import won.protocol.repository.MessageEventRepository;

@Component
public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    protected ConnectionContainerRepository connectionContainerRepository;
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

    public Optional<MessageEvent> getMessage(URI messageURI) {
        return messageEventRepository.findOneByMessageURI(messageURI);
    }

    public MessageEvent getMessageRequired(URI messageURI) {
        return getMessage(messageURI).orElseThrow(() -> new NoSuchMessageException(messageURI));
    }

    /**
     * If we are saving response message, update original massage with the
     * information about response message uri
     * 
     * @param message response message
     */
    @Transactional(propagation = Propagation.MANDATORY)
    // TODO: we should be able to remove this method after the message refactoring
    public void updateResponseInfo(final WonMessage message) {
        // find a message it responds to
        URI originalMessageURI = message.getIsResponseToMessageURI();
        if (originalMessageURI != null) {
            // update the message it responds to with the uri of the response
            messageEventRepository.lockConnectionAndMessageContainerByContainedMessageForUpdate(originalMessageURI);
            messageEventRepository.lockAtomAndMessageContainerByContainedMessageForUpdate(originalMessageURI);
            MessageEvent event = messageEventRepository.findOneByMessageURIforUpdate(originalMessageURI);
            if (event != null) {
                // we may not have saved the event yet if the current message is a
                // FailureResponse
                // and the error causing the response happened before saving the original
                // message.
                event.setResponseMessageURI(message.getMessageURI());
                messageEventRepository.save(event);
            }
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveMessage(final WonMessage wonMessage, URI parent) {
        logger.debug("STORING message with uri {} and parent uri", wonMessage.getMessageURI(), parent);
        MessageContainer container = loadOrCreateMessageContainer(parent, wonMessage.getMessageType());
        DatasetHolder datasetHolder = new DatasetHolder(wonMessage.getMessageURI(),
                        WonMessageEncoder.encodeAsDataset(wonMessage));
        MessageEvent event = new MessageEvent(parent, wonMessage, container);
        MessageInContainer mic = new MessageInContainer(event, container);
        event.setDatasetHolder(datasetHolder);
        container.getEvents().add(mic);
        messageEventRepository.save(event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveMessageInContainer(final URI messageURI, URI parentURI) {
        logger.debug("STORING message with uri {} and parent uri", messageURI, parentURI);
        MessageContainer container = messageContainerRepository.findOneByParentUriForUpdate(parentURI)
                        .orElseThrow(() -> new IncoherentDatabaseStateException(
                                        "Cannot store message " + messageURI + " in container with parent " + parentURI
                                                        + ": container not found"));
        MessageEvent message = messageEventRepository.findOneByMessageURI(messageURI)
                        .orElseThrow(() -> new IncoherentDatabaseStateException(
                                        "Cannot store message " + messageURI + " in container with parent " + parentURI
                                                        + ": message not found"));
        MessageInContainer mic = new MessageInContainer(message, container);
        container.getEvents().add(mic);
        messageContainerRepository.save(container);
    }

    @Transactional(propagation = Propagation.MANDATORY)
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
            connectionMessageContainerRepository.saveAndFlush(cec);
            return connectionMessageContainerRepository.findOne(cec.getId());
        }
        return messageContainerRepository.findOneByParentUriForUpdate(parent)
                        .orElseThrow(() -> new IncoherentDatabaseStateException(
                                        "Cannot store '" + messageType + "' event: unable to find "
                                                        + "event container with parent URI '" + parent + "'"));
    }
}
