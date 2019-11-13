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
package won.bot.framework.eventbot.action.impl.socket;

import java.net.URI;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.NodeIterator;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomDeactivatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherAtomEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.node.socket.impl.WON_TX;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

/**
 * Expects a CloseFromOtherAtom event from a and closes the local atom. If the
 * additional message content of the event ss not a
 * WON_TX.COORDINATION_MESSAGE_COMMIT, an exception is thrown.
 */
public class TwoPhaseCommitDeactivateOnCloseAction extends BaseEventBotAction {
    public TwoPhaseCommitDeactivateOnCloseAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        // If we receive a close event, it must carry a commit message.
        if (event instanceof CloseFromOtherAtomEvent) {
            URI atomURI = ((CloseFromOtherAtomEvent) event).getAtomURI();
            WonMessage wonMessage = ((CloseFromOtherAtomEvent) event).getWonMessage();
            NodeIterator ni = RdfUtils.visitFlattenedToNodeIterator(wonMessage.getMessageContent(),
                            model -> model.listObjectsOfProperty(
                                            model.createProperty(WON_TX.COORDINATION_MESSAGE.getURI())));
            assert ni.hasNext() : "no additional content found in close message, expected a commit";
            String coordinationMessageUri = ni.toList().get(0).asResource().getURI();
            assert coordinationMessageUri.equals(WON_TX.COORDINATION_MESSAGE_COMMIT.getURI()) : "expected a "
                            + "Commit message";
            getEventListenerContext().getWonMessageSender().sendWonMessage(createWonMessage(atomURI));
            getEventListenerContext().getEventBus().publish(new AtomDeactivatedEvent(atomURI));
        }
    }

    private WonMessage createWonMessage(URI atomURI) throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        Dataset ds = getEventListenerContext().getLinkedDataSource().getDataForResource(atomURI);
        URI localWonNode = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(ds, atomURI);
        URI messageURI = wonNodeInformationService.generateEventURI(localWonNode);
        return WonMessageBuilder
                        .deactivate(messageURI)
                        .direction().fromOwner()
                        .atom(atomURI)
                        .build();
    }
}
