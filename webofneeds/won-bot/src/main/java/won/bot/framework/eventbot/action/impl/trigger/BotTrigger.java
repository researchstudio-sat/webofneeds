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
package won.bot.framework.eventbot.action.impl.trigger;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.scheduling.support.PeriodicTrigger;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;

/**
 * Publishes BotTriggerEvents on the eventBus at configurable intervals. Upon
 * call to activate() the BotTrigger registers an event listener for the
 * StartBotTrigger event and the StopBotTriggerEvent After receiving the
 * StopBotTriggerEvent, the BotTrigger unregisters all event listeners.
 */
public class BotTrigger {
    private EventListenerContext context;
    private volatile Duration interval;
    private AtomicBoolean active = new AtomicBoolean(false);
    private EventListener startListener;
    private EventListener stopListener;
    private volatile ScheduledFuture<?> cancelableTask;

    public BotTrigger(EventListenerContext context, Duration interval) {
        this.context = context;
        this.interval = interval;
    }

    /**
     * Sets the interval to the specified duration and reschedules executions.
     *
     * @param interval
     */
    public void changeIntervalTo(Duration interval) {
        this.interval = interval;
        reschedule();
    }

    public Duration getInterval() {
        return interval;
    }

    /**
     * Changes the interval by the specified percentage, or sets it to the specified
     * maxInterval, whichever is smaller.
     *
     * @param factor
     */
    public void changeIntervalByFactor(double factor, Duration maxInterval) {
        long millis = this.interval.toMillis();
        if (millis == 0 && factor < 1) {
            // nothing to do, the interval is already minimial
            return;
        } else if (factor == 1) {
            // nothing to do, no need to reschedule
            return;
        }
        if (factor <= 0)
            throw new IllegalArgumentException("factor must be > 0");
        long newMillis = (long) ((double) millis * factor);
        if (newMillis == millis) {
            // for some reason, there was no change. increase/decreaese by 1
            newMillis += factor > 1 ? 1 : -1;
        }
        newMillis = Math.max(0, Math.min(newMillis, maxInterval.toMillis()));
        this.interval = Duration.ofMillis(newMillis);
        reschedule();
    }

    public void changeIntervalByFactor(double factor) {
        changeIntervalByFactor(factor, Duration.ofSeconds(10));
    }

    public synchronized void activate() {
        if (active.get())
            return;
        // make the stop listener
        this.stopListener = new ActionOnFirstEventListener(this.context, new BotTriggerFilter(this),
                new BaseEventBotAction(BotTrigger.this.context) {
                    @Override
                    protected void doRun(Event event, EventListener executingListener) throws Exception {
                        // unregister all listeners
                        BotTrigger.this.context.getEventBus().unsubscribe(BotTrigger.this.startListener);
                        BotTrigger.this.context.getEventBus().unsubscribe(BotTrigger.this.stopListener);
                        BotTrigger.this.cancelableTask.cancel(true);
                        BotTrigger.this.active.set(false);
                    }
                });
        // make the start listener
        this.startListener = new ActionOnFirstEventListener(this.context, new BotTriggerFilter(this),
                new BaseEventBotAction(BotTrigger.this.context) {
                    @Override
                    protected void doRun(Event event, EventListener executingListener) throws Exception {
                        reschedule();
                    }
                });
        // register both listeners
        context.getEventBus().subscribe(StopBotTriggerCommandEvent.class, stopListener);
        context.getEventBus().subscribe(StartBotTriggerCommandEvent.class, startListener);
        active.set(true);
    }

    private synchronized void reschedule() {
        if (cancelableTask != null) {
            cancelableTask.cancel(true);
        }
        // make the trigger
        PeriodicTrigger myTrigger = new PeriodicTrigger(getInterval().toMillis());
        myTrigger.setInitialDelay(getInterval().toMillis());
        // schedule a task that publishes the BotTriggerEvent
        BotTrigger.this.cancelableTask = context.getTaskScheduler().schedule(new Runnable() {
            @Override
            public void run() {
                fire();
            }
        }, myTrigger);
    }

    protected void fire() {
        BotTrigger.this.context.getEventBus().publish(new BotTriggerEvent(BotTrigger.this));
    }
}
