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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.lifecycle.ErrorEvent;
import won.bot.framework.eventbot.event.impl.listener.FinishedEvent;
import won.bot.framework.eventbot.filter.EventFilter;

import java.lang.invoke.MethodHandles;

/**
 * Base class for event listeners
 */
public abstract class BaseEventListener implements EventListener {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private EventListenerContext context;
    private int eventCount = 0;
    private int exceptionCount = 0;
    private long millisExecuting = 0;
    private boolean unsubscribeOnException = true;
    protected EventFilter eventFilter = null;
    protected String name = getClass().getSimpleName();

    /**
     * Constructor is private so that subclasses must implement the one-arg
     * constructor.
     */
    private BaseEventListener() {
    }

    protected BaseEventListener(final EventListenerContext context) {
        this.context = context;
    }

    protected BaseEventListener(final EventListenerContext context, final EventFilter eventFilter) {
        this(context);
        this.eventFilter = eventFilter;
    }

    protected BaseEventListener(final EventListenerContext context, final String name) {
        this(context);
        this.name = name;
    }

    protected BaseEventListener(final EventListenerContext context, final String name, final EventFilter eventFilter) {
        this(context, eventFilter);
        this.name = name;
    }

    @Override
    public final void onEvent(final Event event) throws Exception {
        if (!shouldHandleEvent(event)) {
            // allow for ignoring events. Such event are not counted.
            return;
        }
        logger.debug("handling event {} with listener {}", event, this);
        countEvent(event);
        long startTime = System.currentTimeMillis();
        try {
            doOnEvent(event);
        } catch (Throwable e) {
            logger.warn("Caught Throwable during event processing by EventListener. Swallowing and publishing an ErrorEvent",
                            e);
            if (unsubscribeOnException) {
                context.getEventBus().unsubscribe(this);
            }
            context.getEventBus().publish(new ErrorEvent(e));
            countThrowable(e);
        } finally {
            noteTimeExecuting(startTime);
        }
    }

    /**
     * Publishes an event indicating that the listener is finished. Useful for
     * chaining listeners. Only use when this is really the case.
     */
    protected void publishFinishedEvent() {
        getEventListenerContext().getEventBus().publish(new FinishedEvent(this));
    }

    public long getMillisExecuting() {
        return millisExecuting;
    }

    public int getExceptionCount() {
        return exceptionCount;
    }

    public int getEventCount() {
        return eventCount;
    }

    public boolean isUnsubscribeOnException() {
        return unsubscribeOnException;
    }

    public void setUnsubscribeOnException(final boolean unsubscribeOnException) {
        this.unsubscribeOnException = unsubscribeOnException;
    }

    protected synchronized void countThrowable(final Throwable e) {
        this.exceptionCount++;
    }

    private synchronized void noteTimeExecuting(final long startTime) {
        this.millisExecuting += System.currentTimeMillis() - startTime;
    }

    private synchronized void countEvent(final Event event) {
        this.eventCount++;
    }

    protected abstract void doOnEvent(Event event) throws Exception;

    protected EventListenerContext getEventListenerContext() {
        return context;
    }

    /**
     * Determines whether the given event should be processed or ignored. If it is
     * ignored, it is not counted and does not influence the listener's behavior.
     * The default implementation accepts all events.
     * 
     * @param event
     * @return
     */
    protected final boolean shouldHandleEvent(final Event event) {
        return eventFilter == null ? true : eventFilter.accept(event);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "name='" + name + '\'' + ", eventCount=" + eventCount + '}';
    }
}
