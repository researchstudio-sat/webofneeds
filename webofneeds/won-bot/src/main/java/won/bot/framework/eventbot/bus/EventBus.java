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
package won.bot.framework.eventbot.bus;

import won.bot.framework.eventbot.bus.impl.EventBusStatistics;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;

/**
 * Simple event bus interface. Allows registering for events and publishing
 * events.
 */
public interface EventBus {
    /**
     * Publishes an event. All listeners subscribed for the event will be notified.
     * 
     * @param event
     * @param <T>
     */
    public <T extends Event> void publish(T event);

    /**
     * Subscribes a listener to an event type. If the listener implements the
     * SubscriptionAware interface, its onSubscribe() method will be called before
     * the bot is subscribed.
     * 
     * @param eventClazz
     * @param listener
     * @param <T>
     */
    public <T extends Event> void subscribe(Class<T> eventClazz, EventListener listener);

    /**
     * Unsubscribes a listener from an event type. If the listener implements the
     * SubscriptionAware interface, its onUnsubscribe() method will be called after
     * the bot is unsubscribed.
     * 
     * @param eventClazz
     * @param listener
     * @param <T>
     */
    public <T extends Event> void unsubscribe(Class<T> eventClazz, EventListener listener);

    /**
     * Unsubscribes a listener from all event types it is currently subscribed to.
     * If the listener implements the SubscriptionAware interface, its
     * onUnsubscribe() method will be called after the bot is unsubscribed.
     * 
     * @param listener
     */
    public void unsubscribe(EventListener listener);

    EventBusStatistics generateEventBusStatistics();
}
