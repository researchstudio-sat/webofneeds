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
package won.node.camel.processor.fixed;

import java.net.URI;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WONMSG;

@Component
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_SYSTEM_STRING, messageType = WONMSG.TYPE_DEACTIVATE_STRING)
public class DeactivateMessageFromSystemProcessor extends AbstractCamelProcessor {
    @Override
    public void process(Exchange exchange) throws Exception {
        WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        URI receiverNeedURI = wonMessage.getReceiverNeedURI();
        URI senderNeedURI = wonMessage.getSenderNeedURI();
        if (receiverNeedURI == null) {
            throw new MissingMessagePropertyException(URI.create(WONMSG.RECEIVER_NEED_PROPERTY.toString()));
        }
        if (senderNeedURI == null) {
            throw new MissingMessagePropertyException(URI.create(WONMSG.SENDER_NEED_PROPERTY.toString()));
        }
        if (!receiverNeedURI.equals(senderNeedURI)) {
            throw new IllegalArgumentException("sender need uri " + senderNeedURI + " does not equal receiver need uri "
                            + receiverNeedURI);
        }
        logger.debug("DEACTIVATING need. needURI:{}", receiverNeedURI);
        Need need = DataAccessUtils.loadNeed(needRepository, receiverNeedURI);
        need.getEventContainer().getEvents()
                        .add(messageEventRepository.findOneByMessageURIforUpdate(wonMessage.getMessageURI()));
        need.setState(NeedState.INACTIVE);
        need = needRepository.save(need);
    }
}
