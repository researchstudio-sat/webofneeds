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
import org.apache.camel.Message;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.model.Atom;
import won.protocol.vocabulary.WONMSG;

/**
 * Processor for msg:FromSystem atom messages
 */
@Component
@FixedMessageProcessor(direction = WONMSG.FromSystemString, messageType = WONMSG.AtomMessageString)
public class AtomMessageFromSystemProcessor extends AbstractCamelProcessor {
    @Override
    public void process(Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        URI recipientAtomURI = wonMessage.getRecipientAtomURI();
        URI senderAtomURI = wonMessage.getSenderAtomURI();
        if (recipientAtomURI == null) {
            throw new MissingMessagePropertyException(URI.create(WONMSG.recipientAtom.toString()));
        }
        if (senderAtomURI == null) {
            throw new MissingMessagePropertyException(URI.create(WONMSG.senderAtom.toString()));
        }
        if (!recipientAtomURI.equals(senderAtomURI)) {
            throw new IllegalArgumentException("sender atom uri " + senderAtomURI + " does not equal receiver atom uri "
                            + recipientAtomURI);
        }
        Atom atom = atomRepository.findOneByAtomURI(senderAtomURI);
        if (atom == null) {
            throw new IllegalArgumentException("atom not found - cannot send atom message to: " + senderAtomURI);
        }
        atom.getMessageContainer().getEvents()
                        .add(messageEventRepository.findOneByMessageURIforUpdate(wonMessage.getMessageURI()));
        atom = atomRepository.save(atom);
    }
}
