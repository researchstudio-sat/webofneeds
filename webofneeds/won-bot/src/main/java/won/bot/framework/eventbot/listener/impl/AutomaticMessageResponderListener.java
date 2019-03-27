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
package won.bot.framework.eventbot.listener.impl;

import java.net.URI;
import java.util.Date;

import org.apache.jena.query.Dataset;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.filter.EventFilter;
import won.bot.framework.eventbot.listener.AbstractHandleFirstNEventsListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

/**
 * Listener that responds to open and message events with automatic messages.
 * Can be configured to apply a timeout (non-blocking) before sending messages.
 * Can be configured to send a fixed number of messages and then unsubscribe
 * from events.
 */
public class AutomaticMessageResponderListener extends AbstractHandleFirstNEventsListener {
    private long millisTimeoutBeforeReply = 1000;

    public AutomaticMessageResponderListener(final EventListenerContext context, final int targetNumberOfMessages,
                    final long millisTimeoutBeforeReply) {
        super(context, targetNumberOfMessages);
        this.millisTimeoutBeforeReply = millisTimeoutBeforeReply;
    }

    public AutomaticMessageResponderListener(final EventListenerContext context, final EventFilter eventFilter,
                    final int targetCount, final long millisTimeoutBeforeReply) {
        super(context, eventFilter, targetCount);
        this.millisTimeoutBeforeReply = millisTimeoutBeforeReply;
    }

    public AutomaticMessageResponderListener(final EventListenerContext context, final String name,
                    final int targetCount, final long millisTimeoutBeforeReply) {
        super(context, name, targetCount);
        this.millisTimeoutBeforeReply = millisTimeoutBeforeReply;
    }

    public AutomaticMessageResponderListener(final EventListenerContext context, final String name,
                    final EventFilter eventFilter, final int targetCount, final long millisTimeoutBeforeReply) {
        super(context, name, eventFilter, targetCount);
        this.millisTimeoutBeforeReply = millisTimeoutBeforeReply;
    }

    @Override
    protected void handleFirstNTimes(final Event event) throws Exception {
        if (event instanceof ConnectionSpecificEvent) {
            handleMessageEvent((ConnectionSpecificEvent) event);
        }
    }

    private void handleMessageEvent(final ConnectionSpecificEvent messageEvent) {
        getEventListenerContext().getTaskScheduler().schedule(new Runnable() {
            @Override
            public void run() {
                String incomingMessage = "cannot extract message";
                if (messageEvent instanceof MessageFromOtherNeedEvent) {
                    WonMessage wonMessage = ((MessageFromOtherNeedEvent) messageEvent).getWonMessage();
                    incomingMessage = WonRdfUtils.MessageUtils.getTextMessage(wonMessage);
                }
                String message = createMessage(messageEvent);
                URI connectionUri = messageEvent.getConnectionURI();
                if (logger.isDebugEnabled()) {
                    logger.debug("connection {}: received message: {}", connectionUri, incomingMessage);
                    logger.debug("connection {}: sending  message: {}", connectionUri, message);
                }
                try {
                    getEventListenerContext().getWonMessageSender()
                                    .sendWonMessage(createWonMessage(connectionUri, message));
                } catch (Exception e) {
                    logger.warn("could not send message via connection {}", connectionUri, e);
                }
            }
        }, new Date(System.currentTimeMillis() + this.millisTimeoutBeforeReply));
    }

    private String createMessage(ConnectionSpecificEvent event) {
        String message = "auto reply no " + (getCount());
        if (getTargetCount() > 0) {
            message += " of " + getTargetCount();
        }
        message += "(delay: " + millisTimeoutBeforeReply + " millis) from " + event.getConnectionURI();
        return message;
    }

    @Override
    protected void unsubscribe() {
        logger.debug("unsubscribing from all events");
        getEventListenerContext().getEventBus().unsubscribe(this);
    }

    private WonMessage createWonMessage(URI connectionURI, String message) throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        Dataset connectionRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(connectionURI);
        URI remoteNeed = WonRdfUtils.ConnectionUtils.getRemoteNeedURIFromConnection(connectionRDF, connectionURI);
        URI localNeed = WonRdfUtils.ConnectionUtils.getLocalNeedURIFromConnection(connectionRDF, connectionURI);
        URI wonNode = WonRdfUtils.ConnectionUtils.getWonNodeURIFromConnection(connectionRDF, connectionURI);
        Dataset remoteNeedRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(remoteNeed);
        URI messageURI = wonNodeInformationService.generateEventURI(wonNode);
        return WonMessageBuilder
                        .setMessagePropertiesForConnectionMessage(messageURI, connectionURI, localNeed, wonNode,
                                        WonRdfUtils.ConnectionUtils.getRemoteConnectionURIFromConnection(connectionRDF,
                                                        connectionURI),
                                        remoteNeed,
                                        WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(remoteNeedRDF, remoteNeed), message)
                        .build();
    }
}
