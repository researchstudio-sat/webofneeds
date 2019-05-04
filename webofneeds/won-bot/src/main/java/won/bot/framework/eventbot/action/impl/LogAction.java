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
package won.bot.framework.eventbot.action.impl;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;

/**
 * Outputs a message via the configured logging system.
 */
public class LogAction extends BaseEventBotAction {
    protected String message;

    public LogAction(final EventListenerContext eventListenerContext) {
        this(eventListenerContext, "Log action executed.");
    }

    public LogAction(final EventListenerContext eventListenerContext, final String message) {
        super(eventListenerContext);
        this.message = message;
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        logger.info(message);
    }
}
