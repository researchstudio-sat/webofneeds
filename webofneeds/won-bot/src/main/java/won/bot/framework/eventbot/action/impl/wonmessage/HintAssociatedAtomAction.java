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
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.AtomSpecificEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * BaseEventBotAction connecting two atoms on the specified sockets, or on their
 * default sockets if none are specified. Requires an AtomSpecificEvent to run
 * and expeects the atomURI from the event to be associated with another atom
 * URI via the botContext.saveToObjectMap method.
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
        final URI myAtomUri = ((AtomSpecificEvent) event).getAtomURI();
        final URI targetAtomUri = getEventListenerContext().getBotContextWrapper().getUriAssociation(myAtomUri);
        try {
            logger.info("Sending hint for {} and {}", myAtomUri, targetAtomUri);
            getEventListenerContext().getMatcherProtocolAtomServiceClient().hint(targetAtomUri, myAtomUri, 0.9,
                            matcherURI, null, createWonMessage(targetAtomUri, myAtomUri, 0.9, matcherURI));
        } catch (Exception e) {
            logger.warn("could not send hint for " + myAtomUri + " to " + targetAtomUri, e);
        }
    }

    private WonMessage createWonMessage(URI atomURI, URI otherAtomURI, double score, URI originator)
                    throws WonMessageBuilderException {
        LinkedDataSource linkedDataSource = getEventListenerContext().getLinkedDataSource();
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        URI localWonNode = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(linkedDataSource.getDataForResource(atomURI),
                        atomURI);
        return WonMessageBuilder
                        .setMessagePropertiesForHint(wonNodeInformationService.generateEventURI(localWonNode), atomURI,
                                        localSocketType.map(socketType -> WonLinkedDataUtils
                                                        .getSocketsOfType(atomURI, socketType, linkedDataSource)
                                                        .stream().findFirst().orElse(null)),
                                        localWonNode, otherAtomURI,
                                        targetSocketType.map(socketType -> WonLinkedDataUtils
                                                        .getSocketsOfType(otherAtomURI, socketType, linkedDataSource)
                                                        .stream().findFirst().orElse(null)),
                                        originator, score)
                        .build();
    }
}
