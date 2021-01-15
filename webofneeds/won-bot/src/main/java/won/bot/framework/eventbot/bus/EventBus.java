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

import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.bus.impl.EventBusStatistics;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.filter.EventFilter;
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
    <T extends Event> void publish(T event);

    /**
     * Subscribes a listener to an event type. If the listener implements the
     * SubscriptionAware interface, its onSubscribe() method will be called before
     * the bot is subscribed.
     * 
     * @param eventClazz event-class to listen to
     * @param listener executes Listener if event is fired
     * @param <T> must be a subclass of Event
     * @return a reference to the used eventListener (this reference can be used to
     * unsubscribe the event listener combination)
     */
    <T extends Event> EventListener subscribe(Class<T> eventClazz, EventListener listener);

    /**
     * Subscribes all given actions to an event type, by wrapping a
     * ActionOnEventListener around the given action. If there are multiple actions
     * a new MultipleActions will be wrapped around the actions as well Example:
     * subscribe(Event.class, action1, action2, action3) is equivalent to:
     * subscribe(Event.class, new
     * ActionOnEventListener(action1.getEventListenerContext, new
     * MultipleActions(action1.getEventListenerContext(), action1, action2,
     * action3))) subscribe(Event.class, action) is equivalent to:
     * subscribe(Event.class, new
     * ActionOnEventListener(action.getEventListenerContext(), action))
     *
     * @param eventClazz event-class to listen to
     * @param actions actions to be executed every time the event is fired
     * @param <T> must be a subclass of Event
     * @return a reference to the created eventListener (this reference can be used
     * to unsubscribe the event actions combination)
     */
    <T extends Event> EventListener subscribe(Class<T> eventClazz, final BaseEventBotAction... actions);

    /**
     * Subscribes all given actions to an event type, by wrapping a
     * ActionOnEventListener around the given action. If there are multiple actions
     * a new MultipleActions will be wrapped around the actions as well Example:
     * subscribe(Event.class, filter, action1, action2, action3) is equivalent to:
     * subscribe(Event.class, new
     * ActionOnEventListener(action1.getEventListenerContext, filter, new
     * MultipleActions(action1.getEventListenerContext(), action1, action2,
     * action3))) subscribe(Event.class, filter, action) is equivalent to:
     * subscribe(Event.class, new
     * ActionOnEventListener(action.getEventListenerContext(), filter, action))
     *
     * @param eventClazz event-class to listen to
     * @param filter filter to be applied before actions are executed
     * @param actions actions to be executed every time the event is fired
     * @param <T> must be a subclass of Event
     * @return a reference to the created eventListener (this reference can be used
     * to unsubscribe the event actions combination)
     */
    <T extends Event> EventListener subscribe(Class<T> eventClazz, EventFilter filter,
                    final BaseEventBotAction... actions);

    /**
     * Unsubscribes a listener from an event type. If the listener implements the
     * SubscriptionAware interface, its onUnsubscribe() method will be called after
     * the bot is unsubscribed.
     * 
     * @param eventClazz
     * @param listener
     * @param <T>
     */
    <T extends Event> void unsubscribe(Class<T> eventClazz, EventListener listener);

    /**
     * Unsubscribes a listener from all event types it is currently subscribed to.
     * If the listener implements the SubscriptionAware interface, its
     * onUnsubscribe() method will be called after the bot is unsubscribed.
     * 
     * @param listener
     */
    void unsubscribe(EventListener listener);

    /**
     * Unsubscribes all listeners.
     */
    void clear();

    EventBusStatistics generateEventBusStatistics();
}
