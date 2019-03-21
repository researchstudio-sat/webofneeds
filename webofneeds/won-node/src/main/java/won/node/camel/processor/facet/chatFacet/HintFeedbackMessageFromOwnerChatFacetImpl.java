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

package won.node.camel.processor.facet.chatFacet;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractFromOwnerCamelProcessor;
import won.node.camel.processor.annotation.DefaultFacetMessageProcessor;
import won.node.camel.processor.annotation.FacetMessageProcessor;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim
 * Date: 05.03.2015
 */
@Component @DefaultFacetMessageProcessor(direction = WONMSG.TYPE_FROM_OWNER_STRING, messageType = WONMSG.TYPE_HINT_FEEDBACK_STRING) @FacetMessageProcessor(facetType = WON.CHAT_FACET_STRING, direction = WONMSG.TYPE_FROM_OWNER_STRING, messageType = WONMSG.TYPE_HINT_FEEDBACK_STRING) public class HintFeedbackMessageFromOwnerChatFacetImpl
    extends AbstractFromOwnerCamelProcessor {
  @Override public void process(final Exchange exchange) {
    logger.debug("default facet implementation, not doing anything");
  }
}
