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

package won.bot.framework.eventbot.action.impl.wonmessage;

import java.net.URI;
import java.util.Optional;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.NeedSpecificEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * BaseEventBotAction connecting two needs on the specified facets, or on their default facets if none are specified.
 * Requires a NeedSpecificEvent to run and expeects the needURI from the event to be associated with another need URI
 * via the botContext.saveToObjectMap method.
 */
public class HintAssociatedNeedAction extends BaseEventBotAction {
    private Optional<URI> remoteFacetType = Optional.empty();
    private Optional<URI> localFacetType = Optional.empty();
    private URI matcherURI;

    public HintAssociatedNeedAction(final EventListenerContext eventListenerContext, final URI remoteFacetType,
            final URI localFacetType, final URI matcherURI) {
        super(eventListenerContext);
        this.remoteFacetType = Optional.of(remoteFacetType);
        this.localFacetType = Optional.of(localFacetType);
        this.matcherURI = matcherURI;
    }

    /**
     * 
     * @param eventListenerContext
     * @param matcherURI
     */
    public HintAssociatedNeedAction(EventListenerContext eventListenerContext, URI matcherURI) {
        super(eventListenerContext);
        this.matcherURI = matcherURI;
    }

    @Override
    public void doRun(Event event, EventListener executingListener) {
        if (!(event instanceof NeedSpecificEvent)) {
            logger.error("HintAssociatedNeedAction can only handle NeedSpecificEvents");
            return;
        }
        final URI myNeedUri = ((NeedSpecificEvent) event).getNeedURI();
        final URI remoteNeedUri = getEventListenerContext().getBotContextWrapper().getUriAssociation(myNeedUri);

        try {
            logger.info("Sending hint for {} and {}", myNeedUri, remoteNeedUri);

            getEventListenerContext().getMatcherProtocolNeedServiceClient().hint(remoteNeedUri, myNeedUri, 0.9,
                    matcherURI, null, createWonMessage(remoteNeedUri, myNeedUri, 0.9, matcherURI));

        } catch (Exception e) {
            logger.warn("could not send hint for " + myNeedUri + " to " + remoteNeedUri, e);
        }
    }

    private WonMessage createWonMessage(URI needURI, URI otherNeedURI, double score, URI originator)
            throws WonMessageBuilderException {

        LinkedDataSource linkedDataSource = getEventListenerContext().getLinkedDataSource();

        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();

        URI localWonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(linkedDataSource.getDataForResource(needURI),
                needURI);

        return WonMessageBuilder.setMessagePropertiesForHint(wonNodeInformationService.generateEventURI(localWonNode),
                needURI,
                localFacetType.map(facetType -> WonLinkedDataUtils
                        .getFacetsOfType(needURI, facetType, linkedDataSource).stream().findFirst().orElse(null)),
                localWonNode, otherNeedURI,
                remoteFacetType.map(facetType -> WonLinkedDataUtils
                        .getFacetsOfType(otherNeedURI, facetType, linkedDataSource).stream().findFirst().orElse(null)),
                originator, score).build();
    }

}
