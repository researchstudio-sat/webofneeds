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

import org.apache.jena.query.Dataset;
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
import won.protocol.util.linkeddata.WonLinkedDataUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

/**
 * BaseEventBotAction connecting two atoms on the specified sockets. The atom's
 * URIs are obtained from the bot context. The first two URIs found there are
 * used.
 */
public class ConnectTwoAtomsAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Optional<URI> targetSocketType = Optional.empty();
    private Optional<URI> localSocketType = Optional.empty();
    private String welcomeMessage;

    public ConnectTwoAtomsAction(final EventListenerContext eventListenerContext, final URI targetSocketType,
                    final URI localSocketType, final String welcomeMessage) {
        super(eventListenerContext);
        this.targetSocketType = Optional.of(targetSocketType);
        this.localSocketType = Optional.of(localSocketType);
        this.welcomeMessage = welcomeMessage;
    }

    @Override
    public void doRun(Event event, EventListener executingListener) {
        Collection<URI> atoms = getEventListenerContext().getBotContext().retrieveAllAtomUris();
        try {
            Iterator<?> iter = atoms.iterator();
            getEventListenerContext().getWonMessageSender()
                            .sendWonMessage(createWonMessage((URI) iter.next(), (URI) iter.next()));
        } catch (Exception e) {
            logger.warn("could not connect two atom objects, exception was: ", e);
        }
    }

    private WonMessage createWonMessage(URI fromUri, URI toUri) throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        Dataset localAtomRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(fromUri);
        Dataset targetAtomRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(toUri);
        URI localWonNode = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(localAtomRDF, fromUri);
        URI remoteWonNode = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(targetAtomRDF, toUri);
        return WonMessageBuilder
                        .setMessagePropertiesForConnect(wonNodeInformationService.generateEventURI(localWonNode),
                                        localSocketType.map(socketType -> WonLinkedDataUtils
                                                        .getSocketsOfType(fromUri, socketType,
                                                                        getEventListenerContext().getLinkedDataSource())
                                                        .stream().findFirst().orElse(null)),
                                        fromUri, localWonNode,
                                        targetSocketType.map(socketType -> WonLinkedDataUtils
                                                        .getSocketsOfType(toUri, socketType,
                                                                        getEventListenerContext().getLinkedDataSource())
                                                        .stream().findFirst().orElse(null)),
                                        toUri, remoteWonNode, welcomeMessage)
                        .build();
    }
}
