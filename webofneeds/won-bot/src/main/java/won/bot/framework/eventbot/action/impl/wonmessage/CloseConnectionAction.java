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

import org.apache.jena.query.Dataset;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

/**
 * Listener that will try to obtain a connectionURI from any event passed to it
 * and close that connection.
 */
public class CloseConnectionAction extends BaseEventBotAction {
    private String farewellMessage;

    public CloseConnectionAction(final EventListenerContext context, String farewellMessage) {
        super(context);
        this.farewellMessage = farewellMessage;
    }

    @Override
    protected void doRun(final Event event, EventListener executingListener) throws Exception {
        if (event instanceof ConnectionSpecificEvent) {
            ConnectionSpecificEvent connectionSpecificEvent = (ConnectionSpecificEvent) event;
            logger.debug("trying to close connection related to event {}", connectionSpecificEvent);
            try {
                URI connectionURI = null;
                connectionURI = connectionSpecificEvent.getConnectionURI();
                logger.debug("Extracted connection uri {}", connectionURI);
                if (connectionURI != null) {
                    logger.debug("closing connection {}", connectionURI);
                    getEventListenerContext().getWonMessageSender().sendWonMessage(createWonMessage(connectionURI));
                } else {
                    logger.warn("could not determine which connection to close for event {}", event);
                }
            } catch (Exception e) {
                logger.warn("error trying to close connection", e);
            }
        } else {
            logger.warn("could not determine which connection to close for event {}", event);
        }
    }

    private WonMessage createWonMessage(URI connectionURI) throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        Dataset connectionRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(connectionURI);
        URI targetAtom = WonRdfUtils.ConnectionUtils.getTargetAtomURIFromConnection(connectionRDF, connectionURI);
        URI localAtom = WonRdfUtils.ConnectionUtils.getLocalAtomURIFromConnection(connectionRDF, connectionURI);
        URI wonNode = WonRdfUtils.ConnectionUtils.getWonNodeURIFromConnection(connectionRDF, connectionURI);
        Dataset targetAtomRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(targetAtom);
        return WonMessageBuilder.setMessagePropertiesForClose(wonNodeInformationService.generateEventURI(wonNode),
                        connectionURI, localAtom, wonNode,
                        WonRdfUtils.ConnectionUtils.getTargetConnectionURIFromConnection(connectionRDF, connectionURI),
                        targetAtom, WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(targetAtomRDF, targetAtom),
                        farewellMessage).build();
    }
}
