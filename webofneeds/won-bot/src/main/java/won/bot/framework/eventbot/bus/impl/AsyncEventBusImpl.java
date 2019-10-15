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
package won.bot.framework.eventbot.bus.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.MultipleActions;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.filter.EventFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.SubscriptionAware;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 *
 */
public class AsyncEventBusImpl implements EventBus {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Map<Class<? extends Event>, List<EventListener>> listenerMap = new ConcurrentHashMap<>();
    private Executor executor;
    private Object monitor = new Object();

    public AsyncEventBusImpl(final Executor executor) {
        this.executor = executor;
    }

    @Override
    public <T extends Event> void publish(final T event) {
        logger.debug("publishing event {}", event);
        // get the list of listeners registered for the event
        List<EventListener> listeners = getEventListenersForEvent(event);
        if (listeners == null || listeners.size() == 0) {
            logger.debug("no listeners registered for event {}, ignoring", event);
            return;
        }
        // execute asynchronously: call the event listeners one after another
        callEventListeners(listeners, event);
    }

    @Override
    public <T extends Event> EventListener subscribe(Class<T> clazz, final EventListener listener) {
        logger.debug("subscribing listener {} for type {}", listener, clazz);
        synchronized (monitor) {
            // we want to synchronize so we don't accidentally add or remove listeners at
            // the same time
            List<EventListener> newListenerList = copyOrCreateList(this.listenerMap.get(clazz));
            newListenerList.add(listener);
            this.listenerMap.put(clazz, Collections.unmodifiableList(newListenerList));
            callOnSubscribeIfApplicable(listener, clazz);
        }
        return listener;
    }

    @Override
    public <T extends Event> EventListener subscribe(Class<T> clazz, final BaseEventBotAction... actions) {
        if (actions == null || actions.length == 0) {
            logger.warn("Ignoring event subscription without actions");
            return null;
        } else if (actions.length == 1) {
            return subscribe(clazz, new ActionOnEventListener(actions[0].getEventListenerContext(), actions[0]));
        } else {
            return subscribe(clazz, new ActionOnEventListener(actions[0].getEventListenerContext(),
                            new MultipleActions(actions[0].getEventListenerContext(), actions)));
        }
    }

    @Override
    public <T extends Event> EventListener subscribe(Class<T> clazz, final EventFilter eventFilter,
                    final BaseEventBotAction... actions) {
        if (eventFilter == null) {
            return subscribe(clazz, actions);
        } else if (actions == null || actions.length == 0) {
            logger.warn("Ignoring event subscription without actions");
            return null;
        } else if (actions.length == 1) {
            return subscribe(clazz,
                            new ActionOnEventListener(actions[0].getEventListenerContext(), eventFilter, actions[0]));
        } else {
            return subscribe(clazz, new ActionOnEventListener(actions[0].getEventListenerContext(), eventFilter,
                            new MultipleActions(actions[0].getEventListenerContext(), actions)));
        }
    }

    @Override
    public <T extends Event> void unsubscribe(Class<T> clazz, final EventListener listener) {
        logger.debug("unsubscribing listener {} for type {}", listener, clazz);
        synchronized (monitor) {
            // we want to synchronize so we don't accidentally add or remove listeners at
            // the same time
            List<EventListener> newListenerList = copyOrCreateList(this.listenerMap.get(clazz));
            newListenerList.remove(listener);
            this.listenerMap.put(clazz, Collections.unmodifiableList(newListenerList));
            callOnUnsubscribeIfApplicable(listener, clazz);
        }
    }

    @Override
    public void unsubscribe(final EventListener listener) {
        logger.debug("unsubscribing listener {} from all events", listener);
        synchronized (monitor) {
            for (Map.Entry<Class<? extends Event>, List<EventListener>> entry : listenerMap.entrySet()) {
                boolean unsubscribed = false; // remember if we had to unsubscribe the listener for the current event
                                              // type
                List<EventListener> listeners = entry.getValue();
                if (listeners == null)
                    continue;
                listeners = copyOrCreateList(listeners);
                Iterator<EventListener> it = listeners.iterator();
                while (it.hasNext()) {
                    EventListener subscribedListener = it.next();
                    if (subscribedListener.equals(listener)) {
                        it.remove();
                        unsubscribed = true;
                    }
                }
                entry.setValue(listeners);
                if (unsubscribed) {
                    // if we had to unsubscribe the listener, we may have to call its onUnsubscribe
                    // method
                    callOnUnsubscribeIfApplicable(listener, entry.getKey());
                }
            }
        }
    }

    private void callEventListeners(final List<EventListener> listeners, final Event event) {
        if (listeners == null || listeners.isEmpty())
            return;
        this.executor.execute(() -> {
            logger.debug("processing event {} with {} listeners", event, listeners.size());
            for (EventListener listener : listeners) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    logger.warn("caught exception during execution of event {} on listener {}", event, listener);
                    logger.warn("stacktrace:", e);
                }
            }
        });
    }

    private List<EventListener> getEventListenersForEvent(final Event event) {
        // the map is secured against concurrent modification, the list inside is
        // unmodifiable
        Set<Class<? extends Event>> classes = getEventTypes(event.getClass(), new HashSet<>());
        return listenerMap.entrySet().stream().filter(entry -> classes.contains(entry.getKey()))
                        .flatMap(e -> e.getValue().stream()).collect(Collectors.toList());
    }

    private Set<Class<? extends Event>> getEventTypes(final Class<? extends Event> clazz,
                    Set<Class<? extends Event>> eventTypes) {
        if (eventTypes == null)
            eventTypes = new HashSet<>();
        final Set<Class<? extends Event>> finalEventTypes = eventTypes;
        // add interfaces and recurse for interfaces
        Arrays.stream(clazz.getInterfaces()).forEach(c -> {
            if (Event.class.isAssignableFrom(c)) {
                getEventTypes((Class<? extends Event>) c, finalEventTypes);
            }
        });
        Class superclass = clazz.getSuperclass();
        if (superclass != null && Event.class.isAssignableFrom(superclass)) {
            getEventTypes(superclass, finalEventTypes);
        }
        finalEventTypes.add(clazz);
        return finalEventTypes;
    }

    private List<EventListener> copyOrCreateList(final List<EventListener> listenerList) {
        if (listenerList == null)
            return new ArrayList<>(1);
        List<EventListener> newListenerList = new ArrayList<>(listenerList.size() + 1);
        newListenerList.addAll(listenerList);
        return newListenerList;
    }

    private <T extends Event> void callOnSubscribeIfApplicable(final EventListener listener, final Class<T> clazz) {
        if (listener instanceof SubscriptionAware) {
            ((SubscriptionAware) listener).onSubscribe(clazz);
        }
    }

    private <T extends Event> void callOnUnsubscribeIfApplicable(final EventListener listener, final Class<T> clazz) {
        if (listener instanceof SubscriptionAware) {
            ((SubscriptionAware) listener).onUnsubscribe(clazz);
        }
    }

    @Override
    public EventBusStatistics generateEventBusStatistics() {
        EventBusStatistics statistics = new EventBusStatistics();
        statistics.setListenerCount(listenerMap.values().stream().flatMap(Collection::stream).distinct().count());
        statistics.setListenerCountPerEvent(listenerMap.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().distinct().count())));
        statistics.setListenerCountPerListenerClass(listenerMap.values().stream().flatMap(Collection::stream).distinct()
                        .collect(Collectors.groupingBy(EventListener::getClass, Collectors.counting())));
        return statistics;
    }
}
