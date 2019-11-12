/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.node.camel.processor.general;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.cryptography.rdfsign.SigningStage;
import won.protocol.exception.WonMessageNotWellFormedException;
import won.protocol.exception.WonMessageProcessingException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.WonSignatureData;
import won.protocol.message.processor.impl.WonMessageSignerVerifier;
import won.protocol.model.DatasetHolder;
import won.protocol.model.MessageEvent;
import won.protocol.repository.AtomMessageContainerRepository;
import won.protocol.repository.AtomRepository;
import won.protocol.repository.ConnectionMessageContainerRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.vocabulary.WONMSG;

/**
 * Utility class containing code needed at multiple points for adding references
 * to previous messages to a message.
 */
public class MessageReferencer {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private MessageEventRepository messageEventRepository;
    @Autowired
    private ConnectionRepository connectionRepository;
    @Autowired
    private ConnectionMessageContainerRepository connectionMessageContainerRepository;
    @Autowired
    private AtomRepository atomRepository;
    @Autowired
    private AtomMessageContainerRepository atomMessageContainerRepository;
    @Autowired
    private DatasetHolderRepository datasetHolderRepository;
    @Autowired
    EntityManager entityManager;

    /**
     * Adds message references to <code>message</code>. If
     * <code>messageBeingRepliedTo</code> is not null, a reference to that message
     * will be added. The referencedByOtherMessage flag will be set in all messages
     * that are referenced.
     */
    public WonMessage addMessageReferences(final WonMessage message, URI parentURI)
                    throws WonMessageProcessingException {
        if (true) {
            // TODO rework message referencing
            return message;
        }
        // first, lock the parent. Tthis is required because modifying
        // the past messages may cause conflicing concurrent modifications
        Set<MessageUriAndParentUri> selectedMessageUris = new HashSet<>();
        selectLatestMessage(selectedMessageUris, message, parentURI);
        selectUnreferenceMessages(selectedMessageUris, message, parentURI);
        deselectSelf(selectedMessageUris, message);
        if (logger.isDebugEnabled()) {
            logger.debug("locking parents for message {}", message.getMessageURI());
        }
        lockParentByContainedMessage(message.getMessageURI());
        if (logger.isDebugEnabled()) {
            logger.debug("loading messages to reference for message {}: {}", message.getMessageURI(),
                            selectedMessageUris);
        }
        List<MessageAndPlaceholder> selected = acquireLockedEntities(selectedMessageUris, parentURI);
        WonMessage newMessage = processSelected(selected, message);
        updateReferenced(selected);
        return newMessage;
    }

    private List<MessageAndPlaceholder> acquireLockedEntities(Set<MessageUriAndParentUri> selectedMessageUris,
                    URI parentURI) {
        lockParentByParentURI(parentURI);
        return selectedMessageUris.stream().map(u -> u.getMessageURI()).collect(Collectors.toSet()).stream()
                        // avoid duplicates just to be sure
                        .sorted() // sort to ensure same processing ordering in all transactions (avoid deadlocks)
                        .map(messageUri -> {
                            // lock the messages and load the WonMessages and MessageEventPlaceholders
                            logger.debug("loading for update:{}", messageUri);
                            MessageEvent ev = messageEventRepository.findOneByMessageURIAndParentURIForUpdate(
                                            messageUri,
                                            parentURI);
                            entityManager.refresh(ev);
                            return new MessageAndPlaceholder(ev,
                                            loadWonMessageforURI(messageUri));
                        }).collect(Collectors.toList());
    }

    /**
     * Selects the latest
     *
     * @param selectedUris
     * @param message
     */
    private void selectLatestMessage(Set<MessageUriAndParentUri> selectedUris, WonMessage message,
                    URI parentURI) {
        // initialize a variable for the result
        WonMessage focalMessage = message.getFocalMessage();
        WonMessageType messageType = focalMessage.getMessageType();
        switch (messageType) {
            case SUCCESS_RESPONSE:
            case FAILURE_RESPONSE:
                // the current message is a response message. We want to reference the message
                // we are responding to.
                // we find identify the message with the same parent as our current message and
                // add it
                URI isResponseToURI = focalMessage.getRespondingToMessageURIRequired();
                Optional<MessageEvent> messageEvent = messageEventRepository
                                .findFirstByMessageURIAndParentURI(isResponseToURI, parentURI);
                String methodName = "selectLatestMessage::response";
                if (messageEvent.isPresent()) {
                    selectedUris.add(new MessageUriAndParentUri(messageEvent.get().getMessageURI(),
                                    messageEvent.get().getParentURI()));
                }
                break;
            case CREATE_ATOM:
                // a create message does not reference any other message. It is the root of the
                // message structure
                break;
            default:
                // any other message: reference the latest message in the parent (i.e. the
                // container of the message)
                // if we don't find any: reference the create message of the atom.
                messageEvent = Optional.ofNullable(messageEventRepository.findNewestByParentURI(parentURI));
                if (messageEvent.isPresent()) {
                    selectedUris.add(new MessageUriAndParentUri(messageEvent.get().getMessageURI(),
                                    messageEvent.get().getParentURI()));
                }
        }
    }

    private void lockParentByParentURI(URI parentUri) {
        atomMessageContainerRepository.lockParentAndContainerByParentUriForUpdate(parentUri);
        connectionMessageContainerRepository.lockParentAndContainerByParentUriForUpdate(parentUri);
    }

    private void lockParentByContainedMessage(URI isResponseToURI) {
        messageEventRepository.lockAtomAndMessageContainerByContainedMessageForUpdate(isResponseToURI);
        messageEventRepository.lockConnectionAndMessageContainerByContainedMessageForUpdate(isResponseToURI);
    }

    /**
     * Selects all unreferenced messages for referencing
     */
    private void selectUnreferenceMessages(Set<MessageUriAndParentUri> selectedUris, final WonMessage message,
                    final URI parentURI)
                    throws WonMessageProcessingException {
        if (message.getMessageType() == WonMessageType.CREATE_ATOM)
            return;
        // find all unreferenced messages for the current message's parent
        List<MessageEvent> messageEvents = messageEventRepository
                        .findByParentURIAndNotReferencedByOtherMessage(parentURI);
        // load the WonMessages for the placeholders
        selectedUris.addAll(messageEvents.stream()
                        .map(p -> new MessageUriAndParentUri(p.getMessageURI(), p.getParentURI()))
                        .collect(Collectors.toList()));
    }

    private void deselectSelf(Set<MessageUriAndParentUri> selectedUris, WonMessage message) {
        // the message should not sign itself, just in case:
        selectedUris.removeIf(messageUriAndParentUri -> messageUriAndParentUri.getMessageURI()
                        .equals(message.getMessageURI()));
    }

    private WonMessage processSelected(List<MessageAndPlaceholder> selected, WonMessage message) {
        Dataset messageDataset = message.getCompleteDataset();
        URI outerEnvelopeGraphURI = message.getEnvelopeURI();
        URI messageUri = message.getMessageURI();
        selected.forEach((MessageAndPlaceholder m) -> {
            if (!m.getWonMessage().getMessageTypeRequired().isHintMessage()) {
                // hints are not signed by the matchers, so we can't add their signatures.
                addSignatureReferenceToMessage(messageDataset, messageUri, outerEnvelopeGraphURI, m.getWonMessage());
            }
        });
        WonMessage newMessage = WonMessage.of(messageDataset);
        selected.forEach((MessageAndPlaceholder m) -> {
            newMessage.addMessageProperty(WONMSG.previousMessage, m.getMessageEvent().getMessageURI());
        });
        return newMessage;
    }

    private void updateReferenced(List<MessageAndPlaceholder> selected) {
        selected.stream().forEach((MessageAndPlaceholder m) -> {
            MessageEvent messageEvent = m.getMessageEvent();
            messageEvent.setReferencedByOtherMessage(true);
            messageEventRepository.save(messageEvent);
        });
    }

    private void addSignatureReferenceToMessage(Dataset messageDataset, URI messageURI, URI outerEnvelopeGraphURI,
                    WonMessage msgToLinkTo) {
        SigningStage signingStage = new SigningStage(msgToLinkTo);
        WonSignatureData wonSignatureData = signingStage.getOutermostSignature();
        checkWellformedness(messageURI, msgToLinkTo, wonSignatureData);
        // add them to to outermost envelope in the current message
        WonMessageSignerVerifier.addSignature(wonSignatureData, outerEnvelopeGraphURI.toString(), messageDataset, true);
        if (logger.isDebugEnabled()) {
            logger.debug("adding reference to message {} into message {} ", msgToLinkTo.getMessageURI(), messageURI);
        }
    }

    private void checkWellformedness(final URI messageURI, final WonMessage msgToLinkTo,
                    final WonSignatureData signatureReferences) {
        // there must be exactly one outermost signature, otherwise msgToLinkTo is
        // not well formed - unless it's a hint!
        if (signatureReferences == null && !msgToLinkTo.getMessageTypeRequired().isHintMessage()) {
            throw new WonMessageNotWellFormedException(String.format(
                            "Message %s is not well formed: found no unreferenced "
                                            + "signatures while trying to link to it from message %s",
                            msgToLinkTo.getMessageURI(), messageURI));
        }
    }

    private WonMessage loadWonMessageforURI(final URI messageURI) {
        DatasetHolder datasetHolder = datasetHolderRepository.findOneByUriForUpdate(messageURI)
                        .orElseThrow(() -> new IllegalStateException(
                                        String.format("could not load dataset for message %s", messageURI)));
        entityManager.refresh(datasetHolder);
        return WonMessage.of(datasetHolder.getDataset());
    }

    private static final class MessageAndPlaceholder {
        private MessageEvent messageEvent;
        private WonMessage wonMessage;

        public MessageAndPlaceholder(MessageEvent messageEvent, WonMessage wonMessage) {
            this.messageEvent = messageEvent;
            this.wonMessage = wonMessage;
        }

        public MessageEvent getMessageEvent() {
            return messageEvent;
        }

        public WonMessage getWonMessage() {
            return wonMessage;
        }
    }

    private static final class MessageUriAndParentUri {
        private URI messageURI;
        private URI parentURI;

        public MessageUriAndParentUri(URI messageURI, URI parentURI) {
            this.messageURI = messageURI;
            this.parentURI = parentURI;
        }

        public URI getMessageURI() {
            return messageURI;
        }

        public URI getParentURI() {
            return parentURI;
        }

        @Override
        public String toString() {
            return "{" + "messageURI=" + messageURI + ", parentURI=" + parentURI + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof MessageUriAndParentUri))
                return false;
            MessageUriAndParentUri that = (MessageUriAndParentUri) o;
            if (messageURI != null ? !messageURI.equals(that.messageURI) : that.messageURI != null)
                return false;
            return parentURI != null ? parentURI.equals(that.parentURI) : that.parentURI == null;
        }

        @Override
        public int hashCode() {
            int result = messageURI != null ? messageURI.hashCode() : 0;
            result = 31 * result + (parentURI != null ? parentURI.hashCode() : 0);
            return result;
        }
    }
}
