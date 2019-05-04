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
import java.util.Objects;
import java.util.Optional;

import org.apache.jena.query.Dataset;

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
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * BaseEventBotAction connecting two atoms on the specified sockets or on their
 * default sockets. Requires an AtomSpecificEvent to run and expeects the
 * atomURI from the event to be associated with another atom URI via the
 * botContext.saveToObjectMap method.
 */
public class ConnectWithAssociatedAtomAction extends BaseEventBotAction {
    private Optional<URI> targetSocketType = Optional.empty();
    private Optional<URI> localSocketType = Optional.empty();
    private String welcomeMessage;

    public ConnectWithAssociatedAtomAction(final EventListenerContext eventListenerContext, final URI targetSocketType,
                    final URI localSocketType, String welcomeMessage) {
        super(eventListenerContext);
        Objects.requireNonNull(targetSocketType);
        Objects.requireNonNull(localSocketType);
        this.targetSocketType = Optional.of(targetSocketType);
        this.localSocketType = Optional.of(localSocketType);
        this.welcomeMessage = welcomeMessage;
    }

    public ConnectWithAssociatedAtomAction(EventListenerContext eventListenerContext, String welcomeMessage) {
        super(eventListenerContext);
        this.welcomeMessage = welcomeMessage;
    }

    @Override
    public void doRun(Event event, EventListener executingListener) {
        if (!(event instanceof AtomSpecificEvent)) {
            logger.error("ConnectWithAssociatedAtomAction can only handle AtomSpecificEvents");
            return;
        }
        final URI myAtomUri = ((AtomSpecificEvent) event).getAtomURI();
        final URI targetAtomUri = getEventListenerContext().getBotContextWrapper().getUriAssociation(myAtomUri);
        try {
            getEventListenerContext().getWonMessageSender().sendWonMessage(createWonMessage(myAtomUri, targetAtomUri));
        } catch (Exception e) {
            logger.warn("could not connect " + myAtomUri + " and " + targetAtomUri, e);
        }
    }

    private WonMessage createWonMessage(URI fromUri, URI toUri) throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        Dataset localAtomRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(fromUri);
        Dataset targetAtomRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(toUri);
        URI localWonNode = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(localAtomRDF, fromUri);
        URI remoteWonNode = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(targetAtomRDF, toUri);
        return WonMessageBuilder.setMessagePropertiesForConnect(
                        wonNodeInformationService.generateEventURI(localWonNode),
                        localSocketType.map(socketType -> WonLinkedDataUtils
                                        .getSocketsOfType(fromUri, socketType,
                                                        getEventListenerContext().getLinkedDataSource())
                                        .stream().findFirst().orElse(null)),
                        fromUri, localWonNode,
                        targetSocketType.map(socketType -> WonLinkedDataUtils
                                        .getSocketsOfType(toUri, socketType,
                                                        getEventListenerContext().getLinkedDataSource())
                                        .stream().findFirst().orElse(null)),
                        toUri, remoteWonNode, welcomeMessage).build();
    }
}
