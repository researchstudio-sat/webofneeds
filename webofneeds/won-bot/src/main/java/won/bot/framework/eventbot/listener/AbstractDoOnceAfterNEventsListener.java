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
package won.bot.framework.eventbot.listener;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.filter.EventFilter;

/**
 * Counts how often it is called, offers to call a callback when a certain
 * number is reached. After the target count of events is reached, a
 * FinishedEvent is published. This allows for chaining listeners.
 */
public abstract class AbstractDoOnceAfterNEventsListener extends BaseEventListener implements CountingListener {
    private int targetCount;
    private int count = 0;
    private Object monitor = new Object();
    private boolean finished = false;

    public AbstractDoOnceAfterNEventsListener(final EventListenerContext context, int targetCount) {
        super(context);
        this.targetCount = targetCount;
    }

    protected AbstractDoOnceAfterNEventsListener(final EventListenerContext context, final EventFilter eventFilter,
                    final int targetCount) {
        super(context, eventFilter);
        this.targetCount = targetCount;
    }

    protected AbstractDoOnceAfterNEventsListener(final EventListenerContext context, final String name,
                    final int targetCount) {
        super(context, name);
        this.targetCount = targetCount;
    }

    protected AbstractDoOnceAfterNEventsListener(final EventListenerContext context, final String name,
                    final EventFilter eventFilter, final int targetCount) {
        super(context, name, eventFilter);
        this.targetCount = targetCount;
    }

    @Override
    public void doOnEvent(final Event event) throws Exception {
        boolean doRun = false;
        synchronized (monitor) {
            if (finished) {
                return;
            }
            count++;
            logger.debug("processing event {} of {} (event: {})", new Object[] { count, targetCount, event });
            if (count >= targetCount) {
                logger.debug("calling doOnce");
                doRun = true;
            }
        }
        if (doRun) {
            doOnce(event);
            logger.debug("unsubscribing from event");
            unsubscribe();
            publishFinishedEvent();
            finished = true;
        }
    }

    protected abstract void unsubscribe();

    protected abstract void doOnce(final Event event) throws Exception;

    public int getTargetCount() {
        return targetCount;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{name='" + name + ", count=" + count + ",targetCount=" + targetCount
                        + ", finished=" + finished + '}';
    }
}
