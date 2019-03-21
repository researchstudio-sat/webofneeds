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

package won.node.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.protocol.jms.MessagingService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Need;
import won.protocol.repository.NeedRepository;
import won.protocol.service.WonNodeInformationService;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Manipulates needs from the system side by generating msg:FromSystem messages.
 */
@Component
public class NeedManagementService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private MessagingService messagingService;

  @Autowired
  private WonNodeInformationService wonNodeInformationService;

  @Autowired
  private NeedRepository needRepository;

  public void sendTextMessageToOwner(URI needURI, String message) {
    if (needURI == null) {
      logger.warn("sendTextMessageToOwner called but needUri is null - doing nothing");
      return;
    }
    if (message == null || message.trim().length() == 0) {
      logger.warn("sendTextMessageToOwner called for need {}, but message is null or empty - doing nothing");
      return;
    }
    logger.debug("Sending FromSystem text message to need {}", needURI);

    // check if we have that need (e.g. it's not a need living on another node, or
    // does not exist at all)
    Need need = needRepository.findOneByNeedURI(needURI);
    if (need == null) {
      logger.debug("deactivateNeed called for need {} but that need was not found in the repository - doing nothing");
      return;
    }

    URI wonNodeURI = wonNodeInformationService.getWonNodeUri(needURI);
    if (wonNodeURI == null) {
      logger
          .debug("deactivateNeed called for need {} but we could not find a WonNodeURI for that need - doing nothing");
      return;
    }

    URI messageURI = wonNodeInformationService.generateEventURI(wonNodeURI);
    WonMessageBuilder builder = WonMessageBuilder.setMessagePropertiesForNeedMessageFromSystem(messageURI, needURI,
        wonNodeURI);
    builder.setTextMessage(message);
    sendSystemMessage(builder.build());
  }

  public void deactivateNeed(URI needURI, String optionalMessage) {
    if (needURI == null) {
      logger.warn("deactivateNeed called but needUri is null - doing nothing");
      return;
    }
    logger.debug("Deactivating need {}", needURI);

    // check if we have that need (e.g. it's not a need living on another node, or
    // does not exist at all)
    Need need = needRepository.findOneByNeedURI(needURI);
    if (need == null) {
      logger.debug("deactivateNeed called for need {} but that need was not found in the repository - doing nothing");
      return;
    }
    URI wonNodeURI = wonNodeInformationService.getWonNodeUri(needURI);
    if (wonNodeURI == null) {
      logger
          .debug("deactivateNeed called for need {} but we could not find a WonNodeURI for that need - doing nothing");
      return;
    }
    URI messageURI = wonNodeInformationService.generateEventURI(wonNodeURI);

    WonMessageBuilder builder = WonMessageBuilder.setMessagePropertiesForDeactivateFromSystem(messageURI, needURI,
        wonNodeURI);
    if (optionalMessage != null && optionalMessage.trim().length() > 0) {
      builder.setTextMessage(optionalMessage);
    }
    sendSystemMessage(builder.build());
  }

  /**
   * Processes the system message (allowing facet implementations) and delivers
   * it, depending on its receiver settings.
   *
   * @param message
   */
  protected void sendSystemMessage(WonMessage message) {
    Map headerMap = new HashMap<String, Object>();
    headerMap.put(WonCamelConstants.MESSAGE_HEADER, message);
    messagingService.sendInOnlyMessage(null, headerMap, null, "seda:SystemMessageIn");
  }

}
