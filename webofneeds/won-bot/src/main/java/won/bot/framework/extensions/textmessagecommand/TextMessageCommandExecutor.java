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
package won.bot.framework.extensions.textmessagecommand;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.BaseAtomAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.MessageEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * Listener that reacts to incoming messages, creating internal bot events for
 * them
 */
public class TextMessageCommandExecutor extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final List<TextMessageCommand> textMessageCommands;

    public TextMessageCommandExecutor(EventListenerContext eventListenerContext,
                    List<TextMessageCommand> textMessageCommands) {
        super(eventListenerContext);
        this.textMessageCommands = textMessageCommands;
    }

    @Override
    protected void doRun(final Event event, EventListener executingListener) throws Exception {
        if (event instanceof BaseAtomAndConnectionSpecificEvent) {
            ConnectionSpecificEvent messageEvent = (ConnectionSpecificEvent) event;
            if (messageEvent instanceof MessageEvent) {
                EventListenerContext ctx = getEventListenerContext();
                EventBus bus = ctx.getEventBus();
                Connection con = ((BaseAtomAndConnectionSpecificEvent) messageEvent).getCon();
                WonMessage msg = ((MessageEvent) messageEvent).getWonMessage();
                String message = extractTextMessageFromWonMessage(msg);
                try {
                    if (message != null) {
                        // Ignore anything that does not contain a textmessage
                        for (TextMessageCommand textMessageCommand : textMessageCommands) {
                            if (textMessageCommand.matchesCommand(message)) {
                                try {
                                    textMessageCommand.execute(con, textMessageCommand.getMatcher(message));
                                    break;
                                } catch (UnsupportedOperationException e) {
                                    logger.warn("TextMessageCommand cant be executed due to: {}", e.getMessage());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // error: send an error message
                    bus.publish(new ConnectionMessageCommandEvent(con, "Did not understand your command '" + message
                                    + "': " + e.getClass().getSimpleName() + ":" + e.getMessage()));
                }
            }
        }
    }

    private String extractTextMessageFromWonMessage(WonMessage wonMessage) {
        if (wonMessage == null)
            return null;
        String message = WonRdfUtils.MessageUtils.getTextMessage(wonMessage);
        return StringUtils.trim(message);
    }
}
