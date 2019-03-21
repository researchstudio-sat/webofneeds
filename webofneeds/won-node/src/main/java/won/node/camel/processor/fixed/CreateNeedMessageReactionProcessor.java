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

package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageReactionProcessor;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Need;
import won.protocol.repository.NeedRepository;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * Reacts to a CREATE message, informing matchers of the newly created need.
 */
@Service @FixedMessageReactionProcessor(direction = WONMSG.TYPE_FROM_OWNER_STRING, messageType = WONMSG.TYPE_CREATE_STRING) public class CreateNeedMessageReactionProcessor
    extends AbstractCamelProcessor {
  @Autowired NeedRepository needRepository;

  @Override public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
    Dataset needContent = wonMessage.getMessageContent();
    URI needUri = getNeedURIFromWonMessage(needContent);
    if (needUri == null) {
      logger.warn("could not obtain needURI from message " + wonMessage.getMessageURI());
      return;
    }
    Need need = needRepository.findOneByNeedURI(needUri);
    try {
      WonMessage newNeedNotificationMessage = makeNeedCreatedMessageForMatcher(need);
      matcherProtocolMatcherClient.needCreated(needUri, ModelFactory.createDefaultModel(), newNeedNotificationMessage);
    } catch (Exception e) {
      logger.warn("could not create NeedCreatedNotification", e);
    }
  }

  private WonMessage makeNeedCreatedMessageForMatcher(final Need need) throws NoSuchNeedException {
    return WonMessageBuilder
        .setMessagePropertiesForNeedCreatedNotification(wonNodeInformationService.generateEventURI(), need.getNeedURI(),
            need.getWonNodeURI()).setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL).build();
  }

  private URI getNeedURIFromWonMessage(final Dataset wonMessage) {
    URI needURI;
    needURI = WonRdfUtils.NeedUtils.getNeedURI(wonMessage);
    if (needURI == null) {
      throw new IllegalArgumentException("at least one RDF node must be of type won:Need");
    }
    return needURI;
  }

}
