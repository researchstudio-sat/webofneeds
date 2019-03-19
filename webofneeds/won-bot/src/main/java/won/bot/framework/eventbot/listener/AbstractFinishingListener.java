/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.bot.framework.eventbot.listener;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.filter.EventFilter;

/**
 * Base class for listeners that eventually stop listening. When the decision is made to finish, a FinishedEvent is
 * published and the listener is unsubscribed from all further events.
 */
public abstract class AbstractFinishingListener extends BaseEventListener implements FinishingListener {
    private Object monitor = new Object();
    private boolean finished = false;

    protected AbstractFinishingListener(final EventListenerContext context) {
        super(context);
    }

    protected AbstractFinishingListener(final EventListenerContext context, final EventFilter eventFilter) {
        super(context, eventFilter);
    }

    protected AbstractFinishingListener(final EventListenerContext context, final String name) {
        super(context, name);
    }

    protected AbstractFinishingListener(final EventListenerContext context, final String name,
            final EventFilter eventFilter) {
        super(context, name, eventFilter);
    }

    @Override
    public void doOnEvent(final Event event) throws Exception {
        boolean doRun = true;
        synchronized (monitor) {
            if (finished) {
                logger.debug("not executing handleEvent() as listener is finished: {}", this);
                return;
            }
            if (isFinished()) {
                logger.debug("not executing handleEvent() as listener's finishing condition is met: {}", this);
                doRun = false;
            }
        }
        if (doRun) {
            handleEvent(event);
        }
        if (isFinished()) {
            logger.debug("performing finishing actions for listener: {}", this);
            performFinish();
        }
    }

    /**
     * Performs all finishing actions and sets the finished flag
     */
    protected void performFinish() {
        synchronized (monitor) {
            if (finished)
                return;
            unsubscribe();
            publishFinishedEvent();
            finished = true;
        }
    }

    protected abstract void unsubscribe();

    protected abstract void handleEvent(final Event event) throws Exception;

    @Override
    public abstract boolean isFinished();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{name='" + name + ", finished=" + finished + '}';
    }
}
