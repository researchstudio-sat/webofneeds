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
package won.bot.framework.eventbot.action.impl.debugbot;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageReceivedOnConnectionEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: fkleedorfer Date: 30.01.14
 */
public class OpenConnectionDebugAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String welcomeMessage;
    private Pattern PATTERN_WAIT = Pattern.compile("wait(\\s+([0-9]{1,2}))?");
    private Pattern PATTERN_DENY = Pattern.compile("deny");
    private Pattern PATTERN_IGNORE = Pattern.compile("ignore");
    private String welcomeHelpMessage;

    public OpenConnectionDebugAction(final EventListenerContext context, final String welcomeMessage,
                    final String welcomeHelpMessage) {
        super(context);
        this.welcomeMessage = welcomeMessage;
        this.welcomeHelpMessage = welcomeHelpMessage;
    }

    @Override
    public void doRun(final Event event, EventListener executingListener) throws Exception {
        if (!(event instanceof ConnectFromOtherAtomEvent)) {
            return;
        }
        if (event instanceof WonMessageReceivedOnConnectionEvent) {
            WonMessage msg = ((WonMessageReceivedOnConnectionEvent) event).getWonMessage();
            String message = WonRdfUtils.MessageUtils.getTextMessage(msg);
            if (message == null) {
                message = "";
            }
            Matcher ignoreMatcher = PATTERN_IGNORE.matcher(message);
            if (ignoreMatcher.find()) {
                logger.debug("not reacting to incoming message of type {} as the welcome message contained 'ignore'",
                                msg.getMessageType());
                return;
            }
            Matcher waitMatcher = PATTERN_WAIT.matcher(message);
            final boolean wait = waitMatcher.find();
            int waitSeconds = 15;
            if (wait && waitMatcher.groupCount() == 2) {
                waitSeconds = Integer.parseInt(waitMatcher.group(2));
            }
            Matcher denyMatcher = PATTERN_DENY.matcher(message);
            final boolean deny = denyMatcher.find();
            ConnectionSpecificEvent connectEvent = (ConnectionSpecificEvent) event;
            logger.debug("auto-replying to connect for connection {}", connectEvent.getConnectionURI());
            URI connectionUri = connectEvent.getConnectionURI();
            String finalWelcomeMessage = welcomeMessage;
            if (wait || deny) {
                finalWelcomeMessage = welcomeMessage + " " + (deny ? "Denying" : "Accepting") + " your request "
                                + (wait ? " after a timeout of " + waitSeconds + " seconds" : "");
            } else {
                finalWelcomeMessage = welcomeMessage + " " + welcomeHelpMessage;
            }
            final WonMessage toSend = deny ? createCloseWonMessage(connectionUri, finalWelcomeMessage)
                            : createOpenWonMessage(connectionUri, finalWelcomeMessage);
            Runnable task = () -> {
                getEventListenerContext().getWonMessageSender().sendWonMessage(toSend);
            };
            if (wait) {
                Date when = new Date(System.currentTimeMillis() + waitSeconds * 1000);
                getEventListenerContext().getTaskScheduler().schedule(task, when);
            } else {
                task.run();
            }
        }
    }

    private WonMessage createOpenWonMessage(URI connectionURI, String message) throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        Dataset connectionRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(connectionURI);
        URI targetAtom = WonRdfUtils.ConnectionUtils.getTargetAtomURIFromConnection(connectionRDF, connectionURI);
        URI localAtom = WonRdfUtils.ConnectionUtils.getLocalAtomURIFromConnection(connectionRDF, connectionURI);
        URI wonNode = WonRdfUtils.ConnectionUtils.getWonNodeURIFromConnection(connectionRDF, connectionURI);
        Dataset targetAtomRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(targetAtom);
        return WonMessageBuilder
                        .setMessagePropertiesForOpen(wonNodeInformationService.generateEventURI(wonNode), connectionURI,
                                        localAtom, wonNode,
                                        WonRdfUtils.ConnectionUtils.getTargetConnectionURIFromConnection(connectionRDF,
                                                        connectionURI),
                                        targetAtom,
                                        WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(targetAtomRDF, targetAtom), message)
                        .build();
    }

    private WonMessage createCloseWonMessage(URI connectionURI, String message) throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        Dataset connectionRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(connectionURI);
        URI targetAtom = WonRdfUtils.ConnectionUtils.getTargetAtomURIFromConnection(connectionRDF, connectionURI);
        URI localAtom = WonRdfUtils.ConnectionUtils.getLocalAtomURIFromConnection(connectionRDF, connectionURI);
        URI wonNode = WonRdfUtils.ConnectionUtils.getWonNodeURIFromConnection(connectionRDF, connectionURI);
        Dataset targetAtomRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(targetAtom);
        return WonMessageBuilder
                        .setMessagePropertiesForClose(wonNodeInformationService.generateEventURI(wonNode),
                                        connectionURI, localAtom, wonNode,
                                        WonRdfUtils.ConnectionUtils.getTargetConnectionURIFromConnection(connectionRDF,
                                                        connectionURI),
                                        targetAtom,
                                        WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(targetAtomRDF, targetAtom), message)
                        .build();
    }
}
