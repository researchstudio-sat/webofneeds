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

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.model.Atom;
import won.protocol.model.AtomState;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WONMSG;

import java.lang.invoke.MethodHandles;
import java.net.URI;

@Component
@FixedMessageProcessor(direction = WONMSG.FromSystemString, messageType = WONMSG.DeactivateMessageString)
public class DeactivateMessageFromSystemProcessor extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void process(Exchange exchange) throws Exception {
        WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
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
        logger.debug("DEACTIVATING atom. atomURI:{}", recipientAtomURI);
        Atom atom = DataAccessUtils.loadAtom(atomRepository, recipientAtomURI);
        atom.getMessageContainer().getEvents()
                        .add(messageEventRepository.findOneByMessageURIforUpdate(wonMessage.getMessageURI()));
        atom.setState(AtomState.INACTIVE);
        atom = atomRepository.save(atom);
    }
}
