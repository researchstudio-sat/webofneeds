/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.node.camel.processor.general;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import won.cryptography.rdfsign.SigningStage;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.WonMessageUtils;
import won.protocol.message.WonSignatureData;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.message.processor.impl.WonMessageSignerVerifier;
import won.protocol.model.DatasetHolder;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class containing code needed at multiple points for adding references to previous messages to a message.
 */
public class MessageReferencer {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private MessageEventRepository messageEventRepository;
    @Autowired
    private DatasetHolderRepository datasetHolderRepository;

    /**
     * Adds message references to <code>message</code>. If <code>messageBeingRepliedTo</code> is not null, a reference to that message will be added.
     * The referencedByOtherMessage flag will be set in all messages that are referenced.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public WonMessage addMessageReferences(final WonMessage message) throws WonMessageProcessingException {
        Set<MessageAndPlaceholder> selected = new HashSet<>();
        selectLatestMessage(selected, message);
        selectUnreferenceMessages(selected, message);
        selected = removeReferenceToSelf(selected, message);
        WonMessage newMessage = processSelected(selected, message);
        updateReferenced(selected);
        return newMessage;
    }


    /**
     * Selects the latest
     * @param selected
     * @param message
     */
    private void selectLatestMessage(Set<MessageAndPlaceholder> selected, WonMessage message) {
        List<MessageEventPlaceholder> messageEventPlaceholders = new ArrayList<>();
                //initialize a variable for the result
        WonMessageType messageType = message.getMessageType();
        switch (messageType){

            case SUCCESS_RESPONSE :
            case FAILURE_RESPONSE :
                //we are replying to a message, so add that to the selected List
                MessageEventPlaceholder messageEventPlaceholder = messageEventRepository.findOneByMessageURIforUpdate(WonMessageUtils.getLocalIsResponseToURI(message));
                if (messageEventPlaceholder != null) {
                    messageEventPlaceholders.add(messageEventPlaceholder);
                }
                break;

            case CREATE_NEED:
                //a create message does not reference any other message. It is the root of the message structure
                break;

            default:
                //any other message: reference the latest message in the parent (i.e. the container of the message)
                //if we don't find any: reference the create message of the need.
                messageEventPlaceholder = messageEventRepository.findNewestByParentURIforUpdate(WonMessageUtils.getParentEntityUri(message));
                if (messageEventPlaceholder != null) {
                    messageEventPlaceholders.add(messageEventPlaceholder);
                } else {
                    //we did not find any message to link to. Choose the create message of the need
                    //we're starting a conversation, link to the create message of the need.
                    List<MessageEventPlaceholder> eventsToSelect = messageEventRepository
                            .findByParentURIAndMessageTypeForUpdate(WonMessageUtils.getParentNeedUri(message), WonMessageType
                                    .CREATE_NEED);
                    messageEventPlaceholders.addAll(eventsToSelect);
                }
        }
        //load the WonMessages for the placeholders
        loadWonMessagesAndAddToSelected(selected, messageEventPlaceholders);
    }


    /**
     * Selects all unreferenced messages for referencing
     */
    private void selectUnreferenceMessages(Set<MessageAndPlaceholder> selected, final WonMessage message) throws WonMessageProcessingException {
        //find all unreferenced messages for the current message's parent
        List<MessageEventPlaceholder> messageEventPlaceholders = messageEventRepository
                .findByParentURIAndNotReferencedByOtherMessageForUpdate(WonMessageUtils.getParentEntityUri(message));
        //load the WonMessages for the placeholders
        loadWonMessagesAndAddToSelected(selected, messageEventPlaceholders);
    }

    private void loadWonMessagesAndAddToSelected(Set<MessageAndPlaceholder> selected, List<MessageEventPlaceholder> messageEventPlaceholders) {
        selected.addAll(messageEventPlaceholders.stream().map(messageEventPlaceholder -> {
            WonMessage msg = loadWonMessageforURI(messageEventPlaceholder.getMessageURI());
            return new MessageAndPlaceholder(messageEventPlaceholder, msg);
        }).collect(Collectors.toSet()));
    }

    private Set<MessageAndPlaceholder> removeReferenceToSelf(Set<MessageAndPlaceholder> messageAndPlaceholders, WonMessage message) {
        //the message should not sign itself, just in case:
        return messageAndPlaceholders
                .stream()
                .filter(mep -> !message.getMessageURI().equals(mep.getMessageEventPlaceholder().getMessageURI()) && !mep.getMessageEventPlaceholder().getMessageURI().equals(message.getCorrespondingRemoteMessageURI()))
                .collect(Collectors.toSet());
    }

    private WonMessage processSelected(Set<MessageAndPlaceholder> selected, WonMessage message) {
        Dataset messageDataset = message.getCompleteDataset();
        URI outerEnvelopeGraphURI = message.getOuterEnvelopeGraphURI();
        URI messageUri = message.getMessageURI();
        selected.forEach((MessageAndPlaceholder m) -> {
            addSignatureReferenceToMessage(messageDataset, messageUri, outerEnvelopeGraphURI, m.getWonMessage());
        });
        WonMessage newMessage = new WonMessage(messageDataset);
        selected.forEach((MessageAndPlaceholder m) -> {
            newMessage.addMessageProperty(WONMSG.HAS_PREVIOUS_MESSAGE_PROPERTY, m.getMessageEventPlaceholder().getMessageURI());
        });
        return newMessage;
    }

    private void updateReferenced(Set<MessageAndPlaceholder> selected) {
        selected.stream().forEach((MessageAndPlaceholder m) -> {
            MessageEventPlaceholder messageEventPlaceholder = m.getMessageEventPlaceholder();
            messageEventPlaceholder.setReferencedByOtherMessage(true);
            messageEventRepository.save(messageEventPlaceholder);
        });
    }

    private void addSignatureReferenceToMessage(Dataset messageDataset, URI messageURI, URI outerEnvelopeGraphURI, WonMessage msgToLinkTo) {
        SigningStage signingStage = new SigningStage(msgToLinkTo);
        WonSignatureData wonSignatureData = signingStage.getOutermostSignature();
        checkWellformedness(messageURI, msgToLinkTo, wonSignatureData);
        //add them to to outermost envelope in the current message
        WonMessageSignerVerifier
                .addSignature(wonSignatureData, outerEnvelopeGraphURI.toString(), messageDataset, true);
        if (logger.isDebugEnabled()) {
            logger.debug("adding reference to message {} into message {} ", msgToLinkTo.getMessageURI(), messageURI);
        }
    }

    private void checkWellformedness(final URI messageURI, final WonMessage msgToLinkTo, final WonSignatureData signatureReferences) {
        //there must be exactly one unreferenced signature, otherwise msgToLinkTo is not well formed
        if (signatureReferences == null) {
            throw new IllegalStateException(String.format("Message %s is not well formed: found no unreferenced " +
                            "signatures while trying to link to it from message %s",
                    msgToLinkTo.getMessageURI(), messageURI));
        }
    }

    private WonMessage loadWonMessageforURI(final URI messageURI) {
        DatasetHolder datasetHolder = datasetHolderRepository.findOneByUriForUpdate(messageURI);
        if (datasetHolder == null || datasetHolder.getDataset() == null) {
            throw new IllegalStateException(String.format("could not load dataset for message %s", messageURI));
        }
        return new WonMessage(datasetHolder.getDataset());
    }

    private static final class MessageAndPlaceholder {
        private MessageEventPlaceholder messageEventPlaceholder;
        private WonMessage wonMessage;

        public MessageAndPlaceholder(MessageEventPlaceholder messageEventPlaceholder, WonMessage wonMessage) {
            this.messageEventPlaceholder = messageEventPlaceholder;
            this.wonMessage = wonMessage;
        }

        public MessageEventPlaceholder getMessageEventPlaceholder() {
            return messageEventPlaceholder;
        }

        public WonMessage getWonMessage() {
            return wonMessage;
        }
    }
}
