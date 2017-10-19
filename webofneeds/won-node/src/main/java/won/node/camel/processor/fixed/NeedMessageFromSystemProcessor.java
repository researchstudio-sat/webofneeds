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
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.model.Need;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * Processor for msg:FromSystem need messages
 */
@Component
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_SYSTEM_STRING, messageType = WONMSG.TYPE_NEED_MESSAGE_STRING)
public class NeedMessageFromSystemProcessor extends AbstractCamelProcessor {
    @Override
    public void process(Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        URI receiverNeedURI = wonMessage.getReceiverNeedURI();
        URI senderNeedURI = wonMessage.getSenderNeedURI();
        if (receiverNeedURI== null){
            throw new MissingMessagePropertyException(URI.create(WONMSG.RECEIVER_NEED_PROPERTY.toString()));
        }
        if (senderNeedURI == null){
            throw new MissingMessagePropertyException(URI.create(WONMSG.SENDER_NEED_PROPERTY.toString()));
        }
        if (!receiverNeedURI.equals(senderNeedURI)) {
            throw new IllegalArgumentException("sender need uri " + senderNeedURI +" does not equal receiver need uri " + receiverNeedURI);
        }
        Need need = needRepository.findOneByNeedURI(senderNeedURI);
        if (need == null){
            throw new IllegalArgumentException("need not found - cannot send need message to: " + senderNeedURI);
        }
        need.getEventContainer().getEvents().add(messageEventRepository.findOneByMessageURIforUpdate(wonMessage.getMessageURI()));
        need = needRepository.save(need);
    }
}
