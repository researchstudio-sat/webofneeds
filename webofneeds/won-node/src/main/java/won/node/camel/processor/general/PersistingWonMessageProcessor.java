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
import won.node.service.DataAccessService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.WonMessageType;
import won.protocol.message.WonMessageUtils;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.model.*;
import won.protocol.repository.ConnectionEventContainerRepository;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.NeedEventContainerRepository;

import java.net.URI;

/**
 * Persists the specified WonMessage.
 */
public class PersistingWonMessageProcessor implements WonMessageProcessor {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  protected MessageEventRepository messageEventRepository;
  @Autowired
  protected ConnectionEventContainerRepository connectionEventContainerRepository;
  @Autowired
  protected NeedEventContainerRepository needEventContainerRepository;
  @Autowired
  protected DatasetHolderRepository datasetHolderRepository;
  @Autowired
  DataAccessService dataAccessService;

  @Override
  // we use READ_COMMITTED because we want to wait for an exclusive lock will
  // accept data written by a concurrent transaction that commits before we read
  public WonMessage process(WonMessage message) throws WonMessageProcessingException {
    URI parentURI = WonMessageUtils.getParentEntityUri(message);
    updateResponseInfo(message);
    saveMessage(message, parentURI);
    return message;
  }

  /**
   * If we are saving response message, update original massage with the
   * information about response message uri
   *
   * @param message response message
   */
  private void updateResponseInfo(final WonMessage message) {
    // find a message it responds to
    URI originalMessageURI = message.getIsResponseToMessageURI();
    if (originalMessageURI != null) {
      // update the message it responds to with the uri of the response
      messageEventRepository.lockConnectionAndEventContainerByContainedMessageForUpdate(originalMessageURI);
      messageEventRepository.lockNeedAndEventContainerByContainedMessageForUpdate(originalMessageURI);
      MessageEventPlaceholder event = messageEventRepository.findOneByMessageURIforUpdate(originalMessageURI);
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

  private void saveMessage(final WonMessage wonMessage, URI parent) {
    logger.debug("STORING message with uri {} and parent uri", wonMessage.getMessageURI(), parent);
    EventContainer container = loadOrCreateEventContainer(wonMessage, parent);
    DatasetHolder datasetHolder = new DatasetHolder(wonMessage.getMessageURI(),
        WonMessageEncoder.encodeAsDataset(wonMessage));
    MessageEventPlaceholder event = new MessageEventPlaceholder(parent, wonMessage, container);
    event.setDatasetHolder(datasetHolder);
    messageEventRepository.save(event);
  }

  private EventContainer loadOrCreateEventContainer(final WonMessage wonMessage, final URI parent) {
    WonMessageType type = wonMessage.getMessageType();
    if (WonMessageType.CREATE_NEED.equals(type)) {
      // create a need event container with null parent (because it will only be
      // persisted at a later point in time)
      EventContainer container = needEventContainerRepository.findOneByParentUriForUpdate(parent);
      if (container != null)
        return container;
      NeedEventContainer nec = new NeedEventContainer(null, parent);
      needEventContainerRepository.saveAndFlush(nec);
      return nec;
    } else if (WonMessageType.CONNECT.equals(type) || WonMessageType.HINT_MESSAGE.equals(type)) {
      // create a connection event container witn null parent (because it will only be
      // persisted at a later point in
      // time)
      EventContainer container = connectionEventContainerRepository.findOneByParentUriForUpdate(parent);
      if (container != null)
        return container;
      ConnectionEventContainer cec = new ConnectionEventContainer(null, parent);
      connectionEventContainerRepository.saveAndFlush(cec);
      return cec;
    }
    EventContainer container = needEventContainerRepository.findOneByParentUriForUpdate(parent);
    if (container != null)
      return container;
    container = connectionEventContainerRepository.findOneByParentUriForUpdate(parent);
    if (container != null)
      return container;
    // let's see if we can find the event conta
    throw new IllegalArgumentException("Cannot store '" + type + "' event '" + wonMessage.getMessageURI()
        + "': unable to find " + "event container with parent URI '" + parent + "'");

  }

}
