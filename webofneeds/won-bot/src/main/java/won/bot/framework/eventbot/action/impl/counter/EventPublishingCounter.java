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
package won.bot.framework.eventbot.action.impl.counter;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.filter.EventFilter;

/**
 * Counter that publishes a CountEvent each time it counts.
 */
public class EventPublishingCounter extends CounterImpl {
    private EventBus eventBus;

    public EventPublishingCounter(String name, int initialCount, EventListenerContext context) {
        super(name, initialCount);
        this.eventBus = context.getEventBus();
    }

    public EventPublishingCounter(String name, EventListenerContext context) {
        super(name);
        this.eventBus = context.getEventBus();
    }

    @Override
    public int increment() {
        int count = super.increment();
        if (eventBus != null) {
            eventBus.publish(new CountEvent(this, count));
        }
        return count;
    }

    @Override
    public int decrement() {
        int count = super.decrement();
        if (eventBus != null) {
            eventBus.publish(new CountEvent(this, count));
        }
        return count;
    }

    public EventFilter makeEventFilter() {
        return new EventFilter() {
            @Override
            public boolean accept(Event event) {
                if (!(event instanceof CountEvent)) {
                    return false;
                }
                return ((CountEvent) event).getCounter() == EventPublishingCounter.this;
            }
        };
    }
}
