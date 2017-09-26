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

package won.bot.framework.eventbot.action.impl.counter;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.filter.EventFilter;

/**
 * User: fkleedorfer
 * Date: 30.04.14
 */
public class TargetCounterDecorator implements Counter
{
  private Counter delegate;
  private int targetCount;
  private EventListenerContext context;

  public TargetCounterDecorator(final EventListenerContext context, final Counter delegate, final int targetCount) {
    this.delegate = delegate;
    this.targetCount = targetCount;
    this.context = context;
  }

  @Override
  public int getCount() {
    return delegate.getCount();
  }

  public int getTargetCount() { return targetCount; }

  @Override
  public int increment() {
    boolean publishEvent = false;
    int cnt = 0;
    synchronized (this) {
      cnt = delegate.increment();
      publishEvent = checkCount(cnt);
    }
    if (publishEvent){
      publishEvent();
    }
    return cnt;

  }

  @Override
  public int decrement() {
    boolean publishEvent = false;
    int cnt = 0;
    synchronized (this) {
      cnt = delegate.decrement();
      publishEvent = checkCount(cnt);
    }
    if (publishEvent){
      publishEvent();
    }
    return cnt;
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  private boolean checkCount(final int cnt) {
    return cnt == targetCount;
  }

  private void publishEvent(){
    this.context.getEventBus().publish(new TargetCountReachedEvent(this));
  }

  public EventFilter makeEventFilter(){
      return new EventFilter() {
          @Override
          public boolean accept(Event event) {
              if (! (event instanceof TargetCountReachedEvent)) {
                  return false;
              }
              return ((TargetCountReachedEvent)event).getCounter() == TargetCounterDecorator.this;
          }
      };
  }

}
