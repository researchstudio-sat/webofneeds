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
package won.bot.framework.eventbot.action;

import java.net.URI;
import java.util.Optional;

import org.apache.jena.query.Dataset;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.AtomHintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SocketHintFromMatcherEvent;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * Created by fkleedorfer on 10.06.2016.
 */
public class BotActionUtils {
    public static WonMessage createWonMessage(final EventListenerContext context, final URI connectionURI,
                    final String message) throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = context.getWonNodeInformationService();
        Dataset connectionRDF = context.getLinkedDataSource().getDataForResource(connectionURI);
        URI targetAtom = WonRdfUtils.ConnectionUtils.getTargetAtomURIFromConnection(connectionRDF, connectionURI);
        URI localAtom = WonRdfUtils.ConnectionUtils.getLocalAtomURIFromConnection(connectionRDF, connectionURI);
        URI wonNode = WonRdfUtils.ConnectionUtils.getWonNodeURIFromConnection(connectionRDF, connectionURI);
        Dataset targetAtomRDF = context.getLinkedDataSource().getDataForResource(targetAtom);
        URI messageURI = wonNodeInformationService.generateEventURI(wonNode);
        return WonMessageBuilder
                        .setMessagePropertiesForConnectionMessage(messageURI, connectionURI, localAtom, wonNode,
                                        WonRdfUtils.ConnectionUtils.getTargetConnectionURIFromConnection(connectionRDF,
                                                        connectionURI),
                                        targetAtom,
                                        WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(targetAtomRDF, targetAtom), message)
                        .build();
    }

    /**
     * Retrieves the recipient atom URI for either AtomHintFromMatcherEvent or
     * SocketHintFromMatcherEvent.
     * 
     * @param event
     * @return
     */
    public static Optional<URI> getTargetAtomURIFromHintEvent(Event event, LinkedDataSource linkedDataSource) {
        if (event instanceof AtomHintFromMatcherEvent) {
            return Optional.of(((AtomHintFromMatcherEvent) event).getHintTargetAtom());
        }
        if (event instanceof SocketHintFromMatcherEvent) {
            URI socketURI = ((SocketHintFromMatcherEvent) event).getHintTargetSocket();
            return WonLinkedDataUtils.getAtomOfSocket(socketURI, linkedDataSource);
        }
        return Optional.empty();
    }

    /**
     * Retrieves the recipient atom URI for either AtomHintFromMatcherEvent or
     * SocketHintFromMatcherEvent.
     * 
     * @param event
     * @return
     */
    public static Optional<URI> getRecipientAtomURIFromHintEvent(Event event, LinkedDataSource linkedDataSource) {
        if (event instanceof AtomHintFromMatcherEvent) {
            return Optional.of(((AtomHintFromMatcherEvent) event).getRecipientAtom());
        }
        if (event instanceof SocketHintFromMatcherEvent) {
            URI socketURI = ((SocketHintFromMatcherEvent) event).getRecipientSocket();
            return WonLinkedDataUtils.getAtomOfSocket(socketURI, linkedDataSource);
        }
        return Optional.empty();
    }
}
