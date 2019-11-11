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

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.AtomHintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SocketHintFromMatcherEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * User: fkleedorfer Date: 30.01.14
 */
public class OpenConnectionAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String welcomeMessage;

    public OpenConnectionAction(final EventListenerContext context, final String welcomeMessage) {
        super(context);
        this.welcomeMessage = welcomeMessage;
    }

    @Override
    public void doRun(final Event event, EventListener executingListener) throws Exception {
        if (event instanceof ConnectFromOtherAtomEvent) {
            ConnectFromOtherAtomEvent connectEvent = (ConnectFromOtherAtomEvent) event;
            logger.debug("auto-replying to connect for connection {}", connectEvent.getConnectionURI());
            getEventListenerContext().getWonMessageSender()
                            .sendWonMessage(createConnectWonMessage(connectEvent.getAtomURI(),
                                            connectEvent.getTargetAtomURI(),
                                            connectEvent.getRecipientSocket(),
                                            connectEvent.getSenderSocket()));
        } else if (event instanceof AtomHintFromMatcherEvent) {
            // TODO: the hint with a match object is not really suitable here. Would be
            // better to
            // use connection object instead
            AtomHintFromMatcherEvent hintEvent = (AtomHintFromMatcherEvent) event;
            logger.debug("opening connection based on hint {}", event);
            URI recipientAtom = hintEvent.getRecipientAtom();
            LinkedDataSource lds = getEventListenerContext().getLinkedDataSource();
            Optional<URI> recipientSocket = WonLinkedDataUtils.getDefaultSocket(hintEvent.getRecipientAtom(), false,
                            lds);
            Optional<URI> targetSocket = WonLinkedDataUtils.getDefaultSocket(hintEvent.getHintTargetAtom(), false, lds);
            if (recipientSocket.isPresent() && targetSocket.isPresent()) {
                getEventListenerContext().getWonMessageSender()
                                .sendWonMessage(createConnectWonMessage(hintEvent.getRecipientAtom(),
                                                hintEvent.getHintTargetAtom(), recipientSocket.get(),
                                                targetSocket.get()));
            }
        } else if (event instanceof SocketHintFromMatcherEvent) {
            // TODO: the hint with a match object is not really suitable here. Would be
            // better to
            // use connection object instead
            SocketHintFromMatcherEvent hintEvent = (SocketHintFromMatcherEvent) event;
            Optional<URI> recipientAtom = Optional.of(hintEvent.getRecipientAtom());
            Optional<URI> hintTargetAtom = Optional.of(hintEvent.getHintTargetAtom());
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
                                            hintEvent.getRecipientSocket(),
                                            (hintEvent.getHintTargetSocket())));
        }
    }

    private WonMessage createConnectWonMessage(URI fromUri, URI toUri, URI localSocket,
                    URI targetSocket) throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        Dataset localAtomRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(fromUri);
        URI localWonNode = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(localAtomRDF, fromUri);
        return WonMessageBuilder.setMessagePropertiesForConnect(
                        wonNodeInformationService.generateEventURI(localWonNode), localSocket,
                        targetSocket, welcomeMessage).build();
    }
}
