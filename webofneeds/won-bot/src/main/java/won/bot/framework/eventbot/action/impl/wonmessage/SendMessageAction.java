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

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.BotActionUtils;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;

/**
 * Action that sends a generic message.
 */
public class SendMessageAction extends BaseEventBotAction {
    private String message = "Hello World";

    public SendMessageAction(final EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    public SendMessageAction(final EventListenerContext eventListenerContext, final String message) {
        super(eventListenerContext);
        this.message = message;
    }

    @Override
    protected void doRun(final Event event, EventListener executingListener) throws Exception {
        if (event instanceof ConnectionSpecificEvent) {
            sendMessage((ConnectionSpecificEvent) event, message);
        }
    }

    protected void sendMessage(final ConnectionSpecificEvent messageEvent, String message) {
        URI connectionUri = messageEvent.getConnectionURI();
        logger.debug("sending message ");
        try {
            getEventListenerContext().getWonMessageSender().sendWonMessage(
                            BotActionUtils.createWonMessage(getEventListenerContext(), connectionUri, message));
        } catch (Exception e) {
            logger.warn("could not send message via connection {}", connectionUri, e);
        }
    }
}
