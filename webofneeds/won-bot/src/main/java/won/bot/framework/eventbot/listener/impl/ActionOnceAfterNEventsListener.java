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
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.filter.EventFilter;
import won.bot.framework.eventbot.listener.AbstractDoOnceAfterNEventsListener;

/**
 * Counts how often it is called, offers to call a callback when a certain
 * number is reached.
 */
public class ActionOnceAfterNEventsListener extends AbstractDoOnceAfterNEventsListener {
    private EventBotAction task;

    public ActionOnceAfterNEventsListener(final EventListenerContext context, int targetCount, EventBotAction task) {
        super(context, targetCount);
        this.task = task;
    }

    public ActionOnceAfterNEventsListener(final EventListenerContext context, final EventFilter eventFilter,
                    final int targetCount, final EventBotAction task) {
        super(context, eventFilter, targetCount);
        this.task = task;
    }

    public ActionOnceAfterNEventsListener(final EventListenerContext context, final String name, final int targetCount,
                    final EventBotAction task) {
        super(context, name, targetCount);
        this.task = task;
    }

    public ActionOnceAfterNEventsListener(final EventListenerContext context, final String name,
                    final EventFilter eventFilter, final int targetCount, final EventBotAction task) {
        super(context, name, eventFilter, targetCount);
        this.task = task;
    }

    @Override
    protected void unsubscribe() {
        getEventListenerContext().getEventBus().unsubscribe(this);
    }

    @Override
    protected void doOnce(Event event) {
        getEventListenerContext().getExecutor().execute(task.getActionTask(event, this));
    }
}
