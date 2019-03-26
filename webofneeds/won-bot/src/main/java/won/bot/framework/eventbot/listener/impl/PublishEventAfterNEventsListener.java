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

package won.bot.framework.eventbot.listener.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.filter.EventFilter;
import won.bot.framework.eventbot.listener.BaseEventListener;

/**
 * EventListener that counts the events it is subscribed for and after having
 * seen a specified number of events, publishes a specified event. After that,
 * counters are reset and the listener will eventually publish another such
 * event.
 */
public class PublishEventAfterNEventsListener<T extends Event> extends BaseEventListener {
  private int count = 0;
  private int targetCount;
  private Class<T> eventClassToPublish;
  private Object monitor = new Object();

  /**
   * @param context
   * @param eventClassToPublish must be a subclass of Event
   * @param targetCount
   */
  public PublishEventAfterNEventsListener(EventListenerContext context, final Class<T> eventClassToPublish,
      final int targetCount) {
    super(context);
    this.targetCount = targetCount;
    this.eventClassToPublish = eventClassToPublish;
  }

  public PublishEventAfterNEventsListener(final EventListenerContext context, final EventFilter eventFilter,
      final Class<T> eventClassToPublish, final int targetCount) {
    super(context, eventFilter);
    this.eventClassToPublish = eventClassToPublish;
    this.targetCount = targetCount;
  }

  public PublishEventAfterNEventsListener(final EventListenerContext context, final String name, final int targetCount,
      final Class<T> eventClassToPublish) {
    super(context, name);
    this.targetCount = targetCount;
    this.eventClassToPublish = eventClassToPublish;
  }

  public PublishEventAfterNEventsListener(final EventListenerContext context, final String name,
      final EventFilter eventFilter, final int targetCount, final Class<T> eventClassToPublish) {
    super(context, name, eventFilter);
    this.targetCount = targetCount;
    this.eventClassToPublish = eventClassToPublish;
  }

  @Override
  public void doOnEvent(final Event event) throws Exception {
    synchronized (this.monitor) {
      this.count++;
      if (this.count >= targetCount) {
        publishEvent();
        rewind();
      }
    }
  }

  private void rewind() {
    this.count = 0;
  }

  private void publishEvent()
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
    Constructor<T> constructor = this.eventClassToPublish.getConstructor(EventListenerContext.class);
    T event = constructor.newInstance(getEventListenerContext());
    getEventListenerContext().getEventBus().publish(event);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{name='" + name + ", count=" + count + ",targetCount=" + targetCount + '}';
  }
}
