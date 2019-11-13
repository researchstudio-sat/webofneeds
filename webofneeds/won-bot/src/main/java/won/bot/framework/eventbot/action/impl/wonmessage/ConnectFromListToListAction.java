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
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * BaseEventBotAction connecting two atoms on the specified sockets. The atom's
 * URIs are obtained from the bot context. The first two URIs found there are
 * used.
 */
public class ConnectFromListToListAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String fromListName;
    private String toListName;
    private Optional<URI> fromSocketType = Optional.empty();
    private Optional<URI> toSocketType = Optional.empty();
    private long millisBetweenCalls;
    private ConnectHook connectHook;
    private String welcomeMessage;

    public ConnectFromListToListAction(EventListenerContext eventListenerContext, String fromListName,
                    String toListName, URI fromSocketType, URI toSocketType, final long millisBetweenCalls,
                    String welcomeMessage) {
        super(eventListenerContext);
        Objects.requireNonNull(fromSocketType);
        Objects.requireNonNull(toSocketType);
        this.fromListName = fromListName;
        this.toListName = toListName;
        this.fromSocketType = Optional.of(fromSocketType);
        this.toSocketType = Optional.of(toSocketType);
        this.millisBetweenCalls = millisBetweenCalls;
        this.welcomeMessage = welcomeMessage;
    }

    public ConnectFromListToListAction(final EventListenerContext eventListenerContext, final String fromListName,
                    final String toListName, final URI fromSocketType, final URI toSocketType,
                    final long millisBetweenCalls, final ConnectHook connectHook, String welcomeMessage) {
        super(eventListenerContext);
        Objects.requireNonNull(fromSocketType);
        Objects.requireNonNull(toSocketType);
        this.fromListName = fromListName;
        this.toListName = toListName;
        this.fromSocketType = Optional.of(fromSocketType);
        this.toSocketType = Optional.of(toSocketType);
        this.millisBetweenCalls = millisBetweenCalls;
        this.connectHook = connectHook;
        this.welcomeMessage = welcomeMessage;
    }

    public ConnectFromListToListAction(EventListenerContext eventListenerContext, String fromListName,
                    String toListName, final long millisBetweenCalls, String welcomeMessage) {
        super(eventListenerContext);
        this.fromListName = fromListName;
        this.toListName = toListName;
        this.millisBetweenCalls = millisBetweenCalls;
        this.welcomeMessage = welcomeMessage;
    }

    public ConnectFromListToListAction(final EventListenerContext eventListenerContext, final String fromListName,
                    final String toListName, final long millisBetweenCalls, final ConnectHook connectHook,
                    String welcomeMessage) {
        super(eventListenerContext);
        this.fromListName = fromListName;
        this.toListName = toListName;
        this.millisBetweenCalls = millisBetweenCalls;
        this.connectHook = connectHook;
        this.welcomeMessage = welcomeMessage;
    }

    @Override
    public void doRun(Event event, EventListener executingListener) {
        List<URI> fromAtoms = getEventListenerContext().getBotContext().getNamedAtomUriList(fromListName);
        List<URI> toAtoms = getEventListenerContext().getBotContext().getNamedAtomUriList(toListName);
        logger.debug("connecting atoms from list \"{}\" ({}) to atoms from list \"{}\" ({})",
                        new Object[] { fromListName, fromAtoms, toListName, toAtoms });
        long start = System.currentTimeMillis();
        long count = 0;
        if (fromListName.equals(toListName)) {
            // only one connection per pair if from-list is to-list
            for (int i = 0; i < fromAtoms.size(); i++) {
                URI fromUri = fromAtoms.get(i);
                for (int j = i + 1; j < fromAtoms.size(); j++) {
                    URI toUri = fromAtoms.get(j);
                    try {
                        count++;
                        performConnect(fromUri, toUri, new Date(start + count * millisBetweenCalls));
                    } catch (Exception e) {
                        logger.warn("could not connect {} and {}", new Object[] { fromUri, toUri }, e);
                    }
                }
            }
        } else {
            for (URI fromUri : fromAtoms) {
                for (URI toUri : toAtoms) {
                    try {
                        count++;
                        logger.debug("tmp: Connect {} with {}", fromUri.toString(), toUri.toString());
                        performConnect(fromUri, toUri, new Date(start + count * millisBetweenCalls));
                    } catch (Exception e) {
                        logger.warn("could not connect {} and {}", new Object[] { fromUri, toUri }, e);
                    }
                }
            }
        }
    }

    private void performConnect(final URI fromUri, final URI toUri, final Date when) throws Exception {
        logger.debug("scheduling connection message for date {}", when);
        getEventListenerContext().getTaskScheduler().schedule(() -> {
            try {
                logger.debug("connecting atoms {} and {}", fromUri, toUri);
                if (connectHook != null) {
                    connectHook.onConnect(fromUri, toUri);
                }
                WonMessage connMessage = createWonMessage(fromUri, toUri);
                getEventListenerContext().getWonMessageSender().sendWonMessage(connMessage);
            } catch (Exception e) {
                logger.warn("could not connect {} and {}", fromUri, toUri); // throws
                logger.warn("caught exception", e);
            }
        }, when);
    }

    private WonMessage createWonMessage(URI fromUri, URI toUri) throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        Dataset localAtomRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(fromUri);
        Dataset targetAtomRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(toUri);
        URI localWonNode = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(localAtomRDF, fromUri);
        URI remoteWonNode = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(targetAtomRDF, toUri);
        URI messageURI = wonNodeInformationService.generateEventURI(localWonNode);
        URI localSocket = fromSocketType.map(socketType -> WonLinkedDataUtils
                        .getSocketsOfType(fromUri, socketType,
                                        getEventListenerContext().getLinkedDataSource())
                        .stream().findFirst())
                        .orElseThrow(() -> new IllegalStateException(
                                        "No suitable sockets found for connect on " + fromUri))
                        .get();
        URI targetSocket = toSocketType.map(socketType -> WonLinkedDataUtils
                        .getSocketsOfType(toUri, socketType,
                                        getEventListenerContext().getLinkedDataSource())
                        .stream().findFirst())
                        .orElseThrow(() -> new IllegalStateException(
                                        "No suitable sockets found for connect on " + fromUri))
                        .get();
        return WonMessageBuilder
                        .connect(messageURI)
                        .sockets()
                        /**/.sender(localSocket)
                        /**/.recipient(targetSocket)
                        .content().text(welcomeMessage)
                        .build();
    }

    public static abstract class ConnectHook {
        public abstract void onConnect(URI fromAtomURI, URI toAtomURI);
    }
}
