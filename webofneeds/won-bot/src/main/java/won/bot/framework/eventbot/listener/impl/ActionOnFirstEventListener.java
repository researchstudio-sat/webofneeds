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

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotAction;
import won.bot.framework.eventbot.filter.EventFilter;

/**
 * Executes its action exactly once, only when the first event is seen, then
 * unregisters.
 */
public class ActionOnFirstEventListener extends ActionOnceAfterNEventsListener {
    public ActionOnFirstEventListener(EventListenerContext context, EventBotAction task) {
        super(context, 1, task);
    }

    public ActionOnFirstEventListener(EventListenerContext context, EventFilter eventFilter, EventBotAction task) {
        super(context, eventFilter, 1, task);
    }

    public ActionOnFirstEventListener(EventListenerContext context, String name, EventBotAction task) {
        super(context, name, 1, task);
    }

    public ActionOnFirstEventListener(EventListenerContext context, String name, EventFilter eventFilter,
                    EventBotAction task) {
        super(context, name, eventFilter, 1, task);
    }
}
