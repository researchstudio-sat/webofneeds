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
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.MessageEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Date;

/**
 * Listener that responds to open and message events with automatic messages.
 * Can be configured to apply a timeout (non-blocking) before sending messages.
 */
public class RespondWithEchoToMessageAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private long millisTimeoutBeforeReply = 0;

    public RespondWithEchoToMessageAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    public RespondWithEchoToMessageAction(final EventListenerContext eventListenerContext,
                    final long millisTimeoutBeforeReply) {
        super(eventListenerContext);
        this.millisTimeoutBeforeReply = millisTimeoutBeforeReply;
    }

    @Override
    protected void doRun(final Event event, EventListener executingListener) throws Exception {
        if (event instanceof ConnectionSpecificEvent) {
            handleMessageEvent((ConnectionSpecificEvent) event);
        }
    }

    private void handleMessageEvent(final ConnectionSpecificEvent messageEvent) {
        getEventListenerContext().getTaskScheduler().schedule(new Runnable() {
            @Override
            public void run() {
                String message = null;
                if (messageEvent instanceof MessageEvent) {
                    message = createMessage(
                                    extractTextMessageFromWonMessage(((MessageEvent) messageEvent).getWonMessage()));
                } else {
                    message = createMessage(null);
                }
                URI connectionUri = messageEvent.getConnectionURI();
                logger.debug("sending message " + message);
                try {
                    getEventListenerContext().getWonMessageSender()
                                    .sendWonMessage(createWonMessage(connectionUri, message));
                } catch (Exception e) {
                    logger.warn("could not send message via connection {}", connectionUri, e);
                }
            }
        }, new Date(System.currentTimeMillis() + this.millisTimeoutBeforeReply));
    }

    private String extractTextMessageFromWonMessage(WonMessage wonMessage) {
        if (wonMessage == null)
            return null;
        return WonRdfUtils.MessageUtils.getTextMessage(wonMessage);
    }

    private String createMessage(String toEcho) {
        if (toEcho == null) {
            return "auto reply (delay: " + millisTimeoutBeforeReply + " millis)";
        } else {
            return "You said: '" + toEcho + "' (delay: " + millisTimeoutBeforeReply + " millis)";
        }
    }

    private WonMessage createWonMessage(URI connectionURI, String textMessage) throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        Dataset connectionRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(connectionURI);
        URI targetAtom = WonRdfUtils.ConnectionUtils.getTargetAtomURIFromConnection(connectionRDF, connectionURI);
        URI localAtom = WonRdfUtils.ConnectionUtils.getLocalAtomURIFromConnection(connectionRDF, connectionURI);
        URI wonNode = WonRdfUtils.ConnectionUtils.getWonNodeURIFromConnection(connectionRDF, connectionURI);
        Dataset targetAtomRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(targetAtom);
        URI messageURI = wonNodeInformationService.generateEventURI(wonNode);
        return WonMessageBuilder.setMessagePropertiesForConnectionMessage(messageURI, connectionURI, localAtom, wonNode,
                        WonRdfUtils.ConnectionUtils.getTargetConnectionURIFromConnection(connectionRDF, connectionURI),
                        targetAtom, WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(targetAtomRDF, targetAtom), textMessage)
                        .build();
    }
}
