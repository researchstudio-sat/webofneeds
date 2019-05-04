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
import java.util.Date;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.BotActionUtils;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;

/**
 * Listener that responds to a message with automatic messages. Can be
 * configured to apply a timeout (non-blocking) before sending messages.
 */
public class RespondToMessageAction extends BaseEventBotAction {
    private long millisTimeoutBeforeReply = 0;
    private String message = null;

    public RespondToMessageAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    public RespondToMessageAction(final EventListenerContext eventListenerContext,
                    final long millisTimeoutBeforeReply) {
        super(eventListenerContext);
        this.millisTimeoutBeforeReply = millisTimeoutBeforeReply;
    }

    public RespondToMessageAction(final EventListenerContext eventListenerContext, final long millisTimeoutBeforeReply,
                    final String message) {
        super(eventListenerContext);
        this.millisTimeoutBeforeReply = millisTimeoutBeforeReply;
        this.message = message;
    }

    public RespondToMessageAction(final EventListenerContext eventListenerContext, final String message) {
        super(eventListenerContext);
        this.message = message;
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
                String message = createMessage();
                URI connectionUri = messageEvent.getConnectionURI();
                if (logger.isDebugEnabled()) {
                    logger.debug("connection {}: received message: {}", connectionUri,
                                    messageEvent.getClass().getSimpleName());
                    logger.debug("connection {}: sending  message: {}", connectionUri, message);
                }
                try {
                    getEventListenerContext().getWonMessageSender().sendWonMessage(
                                    BotActionUtils.createWonMessage(getEventListenerContext(), connectionUri, message));
                } catch (Exception e) {
                    logger.warn("could not send message via connection {}", connectionUri, e);
                }
            }
        }, new Date(System.currentTimeMillis() + this.millisTimeoutBeforeReply));
    }

    private String createMessage() {
        if (message != null)
            return message;
        return "auto reply (delay: " + millisTimeoutBeforeReply + " millis)";
    }
}
