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

import java.util.Date;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;

/**
 * Base action with capabilities for delegating to another action
 */
public abstract class AbstractDelegatingAction extends BaseEventBotAction {
    private EventBotAction delegate;

    public AbstractDelegatingAction(final EventListenerContext eventListenerContext, final EventBotAction delegate) {
        super(eventListenerContext);
        this.delegate = delegate;
        assert delegate != null : "delegate must not be null";
    }

    protected void delegateImmediately(final Event event, EventListener executingListener) {
        getEventListenerContext().getExecutor().execute(delegate.getActionTask(event, executingListener));
    }

    protected void delegateDelayed(final Event event, long delayMillis, EventListener executingListener) {
        Date when = new Date(System.currentTimeMillis() + delayMillis);
        getEventListenerContext().getTaskScheduler().schedule(delegate.getActionTask(event, executingListener), when);
    }
}
