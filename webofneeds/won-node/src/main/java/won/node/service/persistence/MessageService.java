package won.node.service.persistence;

import java.lang.invoke.MethodHandles;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.WonMessageType;
import won.protocol.model.AtomMessageContainer;
import won.protocol.model.ConnectionMessageContainer;
import won.protocol.model.DatasetHolder;
import won.protocol.model.MessageContainer;
import won.protocol.model.MessageEvent;
import won.protocol.repository.AtomMessageContainerRepository;
import won.protocol.repository.ConnectionContainerRepository;
import won.protocol.repository.ConnectionMessageContainerRepository;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.MessageEventRepository;

@Component
public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    protected ConnectionContainerRepository connectionContainerRepository;
    @Autowired
    protected AtomMessageContainerRepository atomMessageContainerRepository;
    @Autowired
    protected ConnectionMessageContainerRepository connectionMessageContainerRepository;
    @Autowired
    protected DatasetHolderRepository datasetHolderRepository;
    @Autowired
    private MessageEventRepository messageEventRepository;

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
        event.setDatasetHolder(datasetHolder);
        container.getEvents().add(event);
        messageEventRepository.save(event);
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
        MessageContainer container = atomMessageContainerRepository.findOneByParentUriForUpdate(parent);
        if (container != null)
            return container;
        container = connectionMessageContainerRepository.findOneByParentUriForUpdate(parent);
        if (container != null)
            return container;
        // let's see if we can find the event conta
        throw new IllegalArgumentException("Cannot store '" + messageType + "' event: unable to find "
                        + "event container with parent URI '" + parent + "'");
    }
}
