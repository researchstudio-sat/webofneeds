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
package won.bot.framework.eventbot.action.impl.matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;

/**
 * BaseEventBotAction that sends a hint message to the first atom in the context
 * to the second.
 */
public class MatchAtomsAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public MatchAtomsAction(final EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        Collection<URI> atoms = getEventListenerContext().getBotContext().retrieveAllAtomUris();
        Iterator<URI> iter = atoms.iterator();
        URI atom1 = iter.next();
        URI atom2 = iter.next();
        logger.debug("matching atoms {} and {}", atom1, atom2);
        logger.debug("getEventListnerContext():" + getEventListenerContext());
        logger.debug("getMatcherService(): " + getEventListenerContext().getMatcherProtocolAtomServiceClient());
        getEventListenerContext().getMatcherProtocolAtomServiceClient().hint(atom1, atom2, 1.0,
                        URI.create("http://example.com/matcher"), null,
                        createWonMessage(atom1, atom2, 1.0, URI.create("http://example.com/matcher")));
    }

    private WonMessage createWonMessage(URI atomURI, URI otherAtomURI, double score, URI originator)
                    throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        URI localWonNode = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(
                        getEventListenerContext().getLinkedDataSource().getDataForResource(atomURI), atomURI);
        return WonMessageBuilder
                        .setMessagePropertiesForHintToAtom(wonNodeInformationService.generateEventURI(localWonNode),
                                        atomURI, localWonNode, otherAtomURI, originator, score)
                        .build();
    }
}
