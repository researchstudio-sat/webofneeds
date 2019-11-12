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

import static won.node.camel.service.WonCamelHelper.*;

import java.lang.invoke.MethodHandles;

import org.apache.camel.Exchange;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageReactionProcessor;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.model.Atom;
import won.protocol.repository.AtomRepository;
import won.protocol.vocabulary.WONMSG;

/**
 * Reacts to a CREATE message, informing matchers of the newly created atom.
 */
@Service
@FixedMessageReactionProcessor(direction = WONMSG.FromOwnerString, messageType = WONMSG.CreateMessageString)
public class CreateAtomMessageReactionProcessor extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    AtomRepository atomRepository;

    @Override
    public void process(final Exchange exchange) throws Exception {
        // Atom atom = getAtomRequired(exchange, atomService);
        Atom atom = atomService.getAtomForMessageRequired(getMessageRequired(exchange), getDirectionRequired(exchange));
        try {
            WonMessage newAtomNotificationMessage = makeAtomCreatedMessageForMatcher(atom);
            matcherProtocolMatcherClient.atomCreated(atom.getAtomURI(), ModelFactory.createDefaultModel(),
                            newAtomNotificationMessage);
        } catch (Exception e) {
            logger.warn("could not create AtomCreatedNotification", e);
        }
    }

    private WonMessage makeAtomCreatedMessageForMatcher(final Atom atom) throws NoSuchAtomException {
        return WonMessageBuilder
                        .setMessagePropertiesForAtomCreatedNotification(wonNodeInformationService.generateEventURI(),
                                        atom.getAtomURI())
                        .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL).build();
    }
}
