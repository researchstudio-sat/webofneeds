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

import org.apache.jena.query.Dataset;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.BotActionUtils;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.AtomHintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SocketHintFromMatcherEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.ConnectionState;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * User: fkleedorfer Date: 30.01.14
 */
public class OpenConnectionAction extends BaseEventBotAction {
    private String welcomeMessage;

    public OpenConnectionAction(final EventListenerContext context, final String welcomeMessage) {
        super(context);
        this.welcomeMessage = welcomeMessage;
    }

    @Override
    public void doRun(final Event event, EventListener executingListener) throws Exception {
        if (event instanceof ConnectFromOtherAtomEvent) {
            ConnectionSpecificEvent connectEvent = (ConnectionSpecificEvent) event;
            logger.debug("auto-replying to connect for connection {}", connectEvent.getConnectionURI());
            getEventListenerContext().getWonMessageSender()
                            .sendWonMessage(createOpenWonMessage(connectEvent.getConnectionURI()));
            return;
        } else if (event instanceof OpenFromOtherAtomEvent) {
            ConnectionSpecificEvent connectEvent = (ConnectionSpecificEvent) event;
            URI connectionState = WonLinkedDataUtils.getConnectionStateforConnectionURI(connectEvent.getConnectionURI(),
                            getEventListenerContext().getLinkedDataSource());
            if (ConnectionState.REQUEST_RECEIVED.getURI().equals(connectionState)) {
                logger.debug("auto-replying to open(REQUEST_RECEIVED) with open for connection {}",
                                connectEvent.getConnectionURI());
                getEventListenerContext().getWonMessageSender()
                                .sendWonMessage(createOpenWonMessage(connectEvent.getConnectionURI()));
            } else {
                // else do not respond - we assume the connection is now established.
            }
            return;
        } else if (event instanceof AtomHintFromMatcherEvent) {
            // TODO: the hint with a match object is not really suitable here. Would be
            // better to
            // use connection object instead
            AtomHintFromMatcherEvent hintEvent = (AtomHintFromMatcherEvent) event;
            logger.debug("opening connection based on hint {}", event);
            getEventListenerContext().getWonMessageSender()
                            .sendWonMessage(createConnectWonMessage(hintEvent.getRecipientAtom(),
                                            hintEvent.getHintTargetAtom(), Optional.empty(), Optional.empty()));
        } else if (event instanceof SocketHintFromMatcherEvent) {
            // TODO: the hint with a match object is not really suitable here. Would be
            // better to
            // use connection object instead
            SocketHintFromMatcherEvent hintEvent = (SocketHintFromMatcherEvent) event;
            Optional<URI> recipientAtom = BotActionUtils.getRecipientAtomURIFromHintEvent(hintEvent,
                            getEventListenerContext().getLinkedDataSource());
            Optional<URI> hintTargetAtom = BotActionUtils.getTargetAtomURIFromHintEvent(hintEvent,
                            getEventListenerContext().getLinkedDataSource());
            if (!recipientAtom.isPresent()) {
                logger.info("could not get recipient atom for hint event {}, cannot connect", event);
                return;
            }
            if (!hintTargetAtom.isPresent()) {
                logger.info("could not get target atom for hint event {}, cannot connect", event);
                return;
            }
            logger.debug("opening connection based on hint {}", event);
            getEventListenerContext().getWonMessageSender()
                            .sendWonMessage(createConnectWonMessage(recipientAtom.get(), hintTargetAtom.get(),
                                            Optional.of(hintEvent.getRecipientSocket()),
                                            Optional.of(hintEvent.getHintTargetSocket())));
        }
    }

    private WonMessage createOpenWonMessage(URI connectionURI) throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        Dataset connectionRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(connectionURI);
        URI targetAtom = WonRdfUtils.ConnectionUtils.getTargetAtomURIFromConnection(connectionRDF, connectionURI);
        URI localAtom = WonRdfUtils.ConnectionUtils.getLocalAtomURIFromConnection(connectionRDF, connectionURI);
        URI wonNode = WonRdfUtils.ConnectionUtils.getWonNodeURIFromConnection(connectionRDF, connectionURI);
        Dataset targetAtomRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(targetAtom);
        return WonMessageBuilder.setMessagePropertiesForOpen(wonNodeInformationService.generateEventURI(wonNode),
                        connectionURI, localAtom, wonNode,
                        WonRdfUtils.ConnectionUtils.getTargetConnectionURIFromConnection(connectionRDF, connectionURI),
                        targetAtom, WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(targetAtomRDF, targetAtom),
                        welcomeMessage).build();
    }

    private WonMessage createConnectWonMessage(URI fromUri, URI toUri, Optional<URI> localSocket,
                    Optional<URI> targetSocket) throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        Dataset localAtomRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(fromUri);
        Dataset targetAtomRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(toUri);
        URI localWonNode = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(localAtomRDF, fromUri);
        URI remoteWonNode = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(targetAtomRDF, toUri);
        return WonMessageBuilder.setMessagePropertiesForConnect(
                        wonNodeInformationService.generateEventURI(localWonNode), localSocket, fromUri, localWonNode,
                        targetSocket, toUri, remoteWonNode, welcomeMessage).build();
    }
}
