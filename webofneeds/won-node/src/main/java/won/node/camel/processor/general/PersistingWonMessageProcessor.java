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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.WonMessageUtils;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;

import java.net.URI;

/**
 * Persists the specified WonMessage.
 */
public class PersistingWonMessageProcessor implements WonMessageProcessor {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  RDFStorageService rdfStorage;
  @Autowired
  protected MessageEventRepository messageEventRepository;

  @Override
  public WonMessage process(WonMessage message) throws WonMessageProcessingException {
    URI parentURI = WonMessageUtils.getParentEntityUri(message);
    saveMessage(message, parentURI);
    updateResponseInfo(message);
    return message;
  }

  /**
   * If we are saving response message, update original massage with the
   * information about response message uri
   * @param message response message
   */
  private void updateResponseInfo(final WonMessage message) {
    // find a message it responds to
    URI originalMessageURI = message.getIsResponseToMessageURI();
    if (originalMessageURI != null) {
      // update the message it responds to with the uri of the response
      MessageEventPlaceholder event = messageEventRepository.findOneByMessageURI(originalMessageURI);
      if (event != null){
        //we may not have saved the event yet if the current message is a FailureResponse
        //and the error causing the response happened before saving the original message.
        event.setResponseMessageURI(message.getMessageURI());
        messageEventRepository.save(event);
      }
    }
  }

  private void saveMessage(final WonMessage wonMessage, URI parent) {
    logger.debug("STORING message with uri {} and parent uri", wonMessage.getMessageURI(), parent);
    rdfStorage.storeDataset(wonMessage.getMessageURI(),
            WonMessageEncoder.encodeAsDataset(wonMessage));
    messageEventRepository.save(new MessageEventPlaceholder(parent,
      wonMessage));
  }

}
