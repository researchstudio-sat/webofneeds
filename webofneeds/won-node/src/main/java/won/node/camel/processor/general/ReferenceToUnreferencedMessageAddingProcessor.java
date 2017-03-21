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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import won.cryptography.rdfsign.SigningStage;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.WonSignatureData;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.message.processor.impl.WonMessageSignerVerifier;
import won.protocol.model.DatasetHolder;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static won.protocol.message.WonMessageType.*;

/**
 * Created by fkleedorfer on 22.07.2016.
 */
public class ReferenceToUnreferencedMessageAddingProcessor implements WonMessageProcessor
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Autowired
  private MessageEventRepository messageEventRepository;
  @Autowired
  private DatasetHolderRepository datasetHolderRepository;

  @Override
  @Transactional(propagation = Propagation.REQUIRED)
  public WonMessage process(final WonMessage message) throws WonMessageProcessingException {
    //find all unreferenced messages for the current message's parent
    List<MessageEventPlaceholder> messageEventPlaceholders = messageEventRepository
      .findByParentURIAndNotReferencedByOtherMessage(message.getSenderURI());

    //initialize a variable for the result
    WonMessageType messageType = message.getMessageType();
    if (
        messageType == CONNECT ||
        messageType ==  OPEN ||
        messageType == HINT_MESSAGE) {
      //when we're starting a conversation, link to the create message of the need.
      messageEventPlaceholders.addAll(messageEventRepository
                                        .findByParentURIAndMessageType(message.getReceiverNeedURI(), WonMessageType
                                          .CREATE_NEED));
      messageEventPlaceholders.addAll(messageEventRepository
                                        .findByParentURIAndMessageType(message.getSenderNeedURI(), WonMessageType
                                          .CREATE_NEED));
    }
    //the message should not sign itself, just in case:
    messageEventPlaceholders = messageEventPlaceholders
      .stream()
      .filter(mep -> !message.getMessageURI().equals(mep.getMessageURI()) && ! mep.getMessageURI().equals(message.getCorrespondingRemoteMessageURI()))
      .collect(
        Collectors.toList());

    Dataset messageDataset = message.getCompleteDataset();

    for (MessageEventPlaceholder messageEventPlaceholder: messageEventPlaceholders){
      //generate signature references for them
      WonMessage msgToLinkTo = loadWonMessageforURI(messageEventPlaceholder.getMessageURI());
      SigningStage signingStage = new SigningStage(msgToLinkTo);
      WonSignatureData wonSignatureData = signingStage.getOutermostSignature();
      checkWellformedness(message, msgToLinkTo, wonSignatureData);
      //add them to to outermost envelope in the current message
      WonMessageSignerVerifier
        .addSignature(wonSignatureData, message
        .getOuterEnvelopeGraphURI().toString(), messageDataset, true);
      message.addMessageProperty(WONMSG.HAS_PREVIOUS_MESSAGE_PROPERTY, msgToLinkTo.getMessageURI());
      //update the message that now is referenced
      //TODO: if at a later processing stage, the current message raises an error, this flag must be reset to false,
      // otherwise the current message may end up not being persisted but the previous message that is referenced here
      // will not be referenced by any subsequent messages.
      messageEventPlaceholder.setReferencedByOtherMessage(true);
    }
    //persist the message
    messageEventRepository.save(messageEventPlaceholders);
    return message;
  }

  public void checkWellformedness(final WonMessage message, final WonMessage msgToLinkTo, final WonSignatureData signatureReferences) {
    //there must be exactly one unreferenced signature, otherwise msgToLinkTo is not well formed
    if (signatureReferences == null){
      throw new IllegalStateException(String.format("Message %s is not well formed: found no unreferenced " +
                                                      "signatures while trying to link to it from message %s",
                                                    msgToLinkTo.getMessageURI(), message.getMessageURI()));
    }
  }

  private WonMessage loadWonMessageforURI(final URI messageURI) {
    DatasetHolder datasetHolder = datasetHolderRepository.findOneByUri(messageURI);
    if (datasetHolder == null || datasetHolder.getDataset() == null){
      throw new IllegalStateException( String.format("could not load dataset for message %s",messageURI));
    }
    return new WonMessage(datasetHolder.getDataset());

  }
}
