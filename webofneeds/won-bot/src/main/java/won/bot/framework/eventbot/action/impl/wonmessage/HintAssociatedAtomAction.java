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
package won.bot.framework.eventbot.action.impl.wonmessage;

import java.net.URI;
import java.util.Optional;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.AtomSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.debugbot.AtomCreatedEventForDebugHint;
import won.bot.framework.eventbot.event.impl.debugbot.HintType;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * BaseEventBotAction connecting two atoms on the specified sockets, or on other
 * compatible sockets, if the Event default sockets if none are specified.
 * Requires an AtomSpecificEvent to run and expeects the atomURI from the event
 * to be associated with another atom URI via the botContext.saveToObjectMap
 * method.
 */
public class HintAssociatedAtomAction extends BaseEventBotAction {
    private Optional<URI> targetSocketType = Optional.empty();
    private Optional<URI> localSocketType = Optional.empty();
    private URI matcherURI;

    public HintAssociatedAtomAction(final EventListenerContext eventListenerContext, final URI targetSocketType,
                    final URI localSocketType, final URI matcherURI) {
        super(eventListenerContext);
        this.targetSocketType = Optional.of(targetSocketType);
        this.localSocketType = Optional.of(localSocketType);
        this.matcherURI = matcherURI;
    }

    /**
     * @param eventListenerContext
     * @param matcherURI
     */
    public HintAssociatedAtomAction(EventListenerContext eventListenerContext, URI matcherURI) {
        super(eventListenerContext);
        this.matcherURI = matcherURI;
    }

    @Override
    public void doRun(Event event, EventListener executingListener) {
        if (!(event instanceof AtomSpecificEvent)) {
            logger.error("HintAssociatedAtomAction can only handle AtomSpecificEvents");
            return;
        }
        HintType hintType = event instanceof AtomCreatedEventForDebugHint
                        ? ((AtomCreatedEventForDebugHint) event).getHintType()
                        : HintType.ATOM_HINT;
        final URI myAtomUri = ((AtomSpecificEvent) event).getAtomURI();
        final URI targetAtomUri = getEventListenerContext().getBotContextWrapper().getUriAssociation(myAtomUri);
        try {
            logger.info("Sending hint for {} and {}", myAtomUri, targetAtomUri);
            Optional<WonMessage> msg = createWonMessage(targetAtomUri, myAtomUri, 0.9, matcherURI, hintType);
            if (msg.isPresent()) {
                getEventListenerContext().getMatcherProtocolAtomServiceClient().hint(targetAtomUri, myAtomUri, 0.9,
                                matcherURI, null, msg.get());
            } else {
                logger.warn("could not send hint for " + myAtomUri + " to " + targetAtomUri
                                + ": message generation failed ");
            }
        } catch (Exception e) {
            logger.warn("could not send hint for " + myAtomUri + " to " + targetAtomUri, e);
        }
    }

    private Optional<WonMessage> createWonMessage(URI atomURI, URI otherAtomURI, double score, URI originator,
                    HintType hintType)
                    throws WonMessageBuilderException {
        LinkedDataSource linkedDataSource = getEventListenerContext().getLinkedDataSource();
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        URI localWonNode = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(linkedDataSource.getDataForResource(atomURI),
                        atomURI);
        if (hintType == HintType.SOCKET_HINT) {
            Optional<URI> sourceSocket = localSocketType.map(socketType -> WonLinkedDataUtils
                            .getSocketsOfType(atomURI, socketType, linkedDataSource).stream().findFirst().orElse(null));
            Optional<URI> targetSocket = targetSocketType.map(
                            socketType -> WonLinkedDataUtils
                                            .getSocketsOfType(otherAtomURI, socketType, linkedDataSource)
                                            .stream().findFirst().orElse(null));
            if (sourceSocket.isPresent() && targetSocket.isPresent()) {
                return Optional.of(WonMessageBuilder
                                .setMessagePropertiesForHintToSocket(
                                                wonNodeInformationService.generateEventURI(localWonNode), atomURI,
                                                sourceSocket.get(), localWonNode, targetSocket.get(), originator, score)
                                .build());
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.of(WonMessageBuilder
                            .setMessagePropertiesForHintToAtom(wonNodeInformationService.generateEventURI(localWonNode),
                                            atomURI, localWonNode, otherAtomURI, originator, score)
                            .build());
        }
    }
}
