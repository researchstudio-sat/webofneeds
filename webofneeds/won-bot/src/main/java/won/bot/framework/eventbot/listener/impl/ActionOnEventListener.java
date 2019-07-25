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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.filter.EventFilter;
import won.bot.framework.eventbot.listener.BaseEventListener;

import java.lang.invoke.MethodHandles;

/**
 * Executes a task when an event is seen. If the property timesToRun > 0, will
 * unregister after that number of events.
 */
public class ActionOnEventListener extends BaseEventListener {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private EventBotAction task;
    private int timesRun = 0;
    private int timesToRun = -1;
    private Object monitor = new Object();

    public ActionOnEventListener(final EventListenerContext context, final EventBotAction task) {
        super(context);
        this.task = task;
    }

    public ActionOnEventListener(final EventListenerContext context, final EventFilter eventFilter,
                    final EventBotAction task) {
        super(context, eventFilter);
        this.task = task;
    }

    public ActionOnEventListener(final EventListenerContext context, final String name, final EventBotAction task) {
        super(context, name);
        this.task = task;
    }

    public ActionOnEventListener(final EventListenerContext context, final String name, final EventFilter eventFilter,
                    final EventBotAction task) {
        super(context, name, eventFilter);
        this.task = task;
    }

    /**
     * @param context
     * @param task
     * @param timesToRun if > 0, listener will unsubscribe from any events after the
     * specified number of executions.
     */
    public ActionOnEventListener(final EventListenerContext context, EventBotAction task, int timesToRun) {
        super(context);
        this.task = task;
        this.timesToRun = timesToRun;
    }

    public ActionOnEventListener(final EventListenerContext context, final EventFilter eventFilter,
                    final EventBotAction task, final int timesToRun) {
        super(context, eventFilter);
        this.timesToRun = timesToRun;
        this.task = task;
    }

    public ActionOnEventListener(final EventListenerContext context, final String name, final EventBotAction task,
                    final int timesToRun) {
        super(context, name);
        this.task = task;
        this.timesToRun = timesToRun;
    }

    public ActionOnEventListener(final EventListenerContext context, final String name, final EventFilter eventFilter,
                    final EventBotAction task, final int timesToRun) {
        super(context, name, eventFilter);
        this.task = task;
        this.timesToRun = timesToRun;
    }

    @Override
    public void doOnEvent(final Event event) throws Exception {
        synchronized (monitor) {
            timesRun++;
            if (timesToRun <= 0) {
                getEventListenerContext().getExecutor().execute(task.getActionTask(event, this));
            } else if (timesRun < timesToRun) {
                logger.debug("scheduling task, execution no {} ", timesRun);
                getEventListenerContext().getExecutor().execute(task.getActionTask(event, this));
            } else if (timesRun == timesToRun) {
                logger.debug("scheduling task, execution no {} (last time)", timesRun);
                getEventListenerContext().getEventBus().unsubscribe(this);
                getEventListenerContext().getExecutor().execute(task.getActionTask(event, this));
                publishFinishedEvent();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        int result = task.hashCode();
        result = 31 * result + timesToRun;
        return result;
    }
}
