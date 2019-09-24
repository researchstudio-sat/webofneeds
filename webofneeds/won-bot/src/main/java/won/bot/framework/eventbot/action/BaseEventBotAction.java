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
package won.bot.framework.eventbot.action;

import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.lifecycle.ErrorEvent;
import won.bot.framework.eventbot.listener.EventListener;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 *
 */
public abstract class BaseEventBotAction implements won.bot.framework.eventbot.action.EventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private EventListenerContext eventListenerContext;
    private static final String EXCEPTION_TAG = "failed";
    private final String stopwatchName = getClass().getName();

    protected BaseEventBotAction(final EventListenerContext eventListenerContext) {
        Objects.requireNonNull(eventListenerContext);
        this.eventListenerContext = eventListenerContext;
    }

    @Override
    public Runnable getActionTask(final Event event, final EventListener eventListener) {
        return () -> {
            Stopwatch stopwatch = SimonManager.getStopwatch(stopwatchName);
            Split split = stopwatch.start();
            try {
                doRun(event, eventListener);
                split.stop();
            } catch (Exception e) {
                eventListenerContext.getEventBus().publish(new ErrorEvent(e));
                split.stop(EXCEPTION_TAG);
            } catch (Throwable t) {
                logger.warn("could not run action {}", stopwatchName, t);
                split.stop(EXCEPTION_TAG);
                throw t;
            }
        };
    }

    public EventListenerContext getEventListenerContext() {
        return eventListenerContext;
    }

    protected abstract void doRun(Event event, EventListener executingListener) throws Exception;
}
