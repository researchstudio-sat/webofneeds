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

package won.bot.framework.events.listener;

import won.bot.framework.events.Event;
import won.bot.framework.events.EventListener;

/**
 * Counts how often it is called, offers to call a callback when a certain number is reached.
 */
public class DelegateOnceAfterNEventsListener extends AbstractDoOnceAfterNEventsListener
{
  private EventListener delegate;

  public DelegateOnceAfterNEventsListener(final EventListenerContext context, int targetCount, EventListener delegate)
  {
    super(context, targetCount);
    this.delegate = delegate;
  }

  public DelegateOnceAfterNEventsListener(final EventListenerContext context, final EventFilter eventFilter, final int targetCount, final EventListener delegate)
  {
    super(context, eventFilter, targetCount);
    this.delegate = delegate;
  }

  public DelegateOnceAfterNEventsListener(final EventListenerContext context, final String name, final int targetCount, final EventListener delegate)
  {
    super(context, name, targetCount);
    this.delegate = delegate;
  }

  public DelegateOnceAfterNEventsListener(final EventListenerContext context, final String name, final EventFilter eventFilter, final int targetCount, final EventListener delegate)
  {
    super(context, name, eventFilter, targetCount);
    this.delegate = delegate;
  }

  @Override
  protected void unsubscribe()
  {
    getEventListenerContext().getEventBus().unsubscribe(this);
  }

  @Override
  protected void doOnce(final Event event) throws Exception
  {
    delegate.onEvent(event);
  }

  @Override
  public String toString()
  {
    return "DelegateOnceAfterNEventsListener{" +
        "delegate=" + delegate +
        '}';
  }
}
