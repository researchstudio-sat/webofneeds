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

package won.bot.framework.events.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import won.bot.framework.events.Event;
import won.bot.framework.events.EventBus;
import won.bot.framework.events.EventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class SchedulerEventBusImpl implements EventBus
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private ConcurrentHashMap<Class<? extends Event>,List<EventListener>> listenerMap = new ConcurrentHashMap<>();
  private TaskScheduler taskScheduler;
  private Object monitor = new Object();

  public SchedulerEventBusImpl(final TaskScheduler taskScheduler)
  {
    this.taskScheduler = taskScheduler;
  }

  @Override
  public <T extends Event> void publish(final T event)
  {
    logger.debug("publishing event {}", event);
    //get the list of listeners registered for the event
    List<EventListener> listeners = getEventListenersForEvent(event);
    if (listeners == null || listeners.size() == 0) {
      logger.debug("no listeners registered for event {}, ignoring", event);
      return;
    }
    //execute asynchronously: call the event listeners one after another
    callEventListeners(listeners, event);
  }


  @Override
  public <T extends Event> void subscribe(Class<T> clazz, final EventListener listener)
  {
    logger.debug("subscribing listener for type {}", clazz);
    synchronized (monitor) {
      //we want to synchronize so we don't accidentally add or remove listeners at the same time
      List<EventListener> newListenerList = copyOrCreateList(this.listenerMap.get(clazz));
      newListenerList.add(listener);
      this.listenerMap.put(clazz, Collections.unmodifiableList(newListenerList));
    }
  }

  @Override
  public <T extends Event> void unsubscribe(Class<T> clazz, final EventListener listener)
  {
    logger.debug("unsubscribing listener for type {}", clazz);
    synchronized (monitor) {
      //we want to synchronize so we don't accidentally add or remove listeners at the same time
      List<EventListener> newListenerList = copyOrCreateList(this.listenerMap.get(clazz));      newListenerList.remove(listener);
      this.listenerMap.put(clazz, Collections.unmodifiableList(newListenerList));
    }
  }


  private void callEventListeners(final List<EventListener> listeners, final Event event)
  {
    this.taskScheduler.schedule(new Runnable(){
      @Override
      public void run()
      {
        for (EventListener listener : listeners) {
          try {
            logger.debug("processing event {} on listener {}", event, listener);
            listener.onEvent(event);
            logger.debug("finished processing event {} on listener {}", event, listener);
          } catch (Exception e) {
            logger.warn("caught exception during execution of event {} on listener {}", event, listener);
          }
        }
      }
    }, new Date());
  }

  private List<EventListener> getEventListenersForEvent(final Event event)
  {
    //the map is secured against concurrent modification, the list inside is unmodifiable
    return listenerMap.get(event.getClass());
  }


  private List<EventListener> copyOrCreateList(final List<EventListener> listenerList)
  {
    if (listenerList == null) return new ArrayList<EventListener>(1);
    List<EventListener> newListenerList = new ArrayList<EventListener>(listenerList.size()+1);
    newListenerList.addAll(listenerList);
    return newListenerList;
  }


}
