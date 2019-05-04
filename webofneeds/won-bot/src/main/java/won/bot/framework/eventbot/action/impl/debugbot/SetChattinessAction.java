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

import static won.bot.framework.eventbot.action.impl.debugbot.SendChattyMessageAction.KEY_CHATTY_CONNECTIONS;

import java.net.URI;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.debugbot.SetChattinessDebugCommandEvent;
import won.bot.framework.eventbot.listener.EventListener;

/**
 * Expects a SetChattinessDebugCommandEvent and sets the chattiness for the
 * connection of the event.
 */
public class SetChattinessAction extends BaseEventBotAction {
    public SetChattinessAction(final EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(final Event event, EventListener executingListener) throws Exception {
        if (event instanceof SetChattinessDebugCommandEvent) {
            SetChattinessDebugCommandEvent chattinessDebugCommandEvent = (SetChattinessDebugCommandEvent) event;
            URI uri = chattinessDebugCommandEvent.getConnectionURI();
            if (chattinessDebugCommandEvent.isChatty()) {
                getEventListenerContext().getBotContext().saveToObjectMap(KEY_CHATTY_CONNECTIONS, uri.toString(), uri);
            } else {
                getEventListenerContext().getBotContext().removeFromObjectMap(KEY_CHATTY_CONNECTIONS, uri.toString());
            }
        }
    }
}
