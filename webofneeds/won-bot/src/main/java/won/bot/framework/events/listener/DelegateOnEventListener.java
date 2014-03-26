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
 * Simple delegating listener.
 */
public class DelegateOnEventListener extends BaseEventListener
{
  private EventListener delegate;

  public DelegateOnEventListener(final EventListenerContext context, final EventListener delegate)
  {
    super(context);
    this.delegate = delegate;
  }

  public DelegateOnEventListener(final EventListenerContext context, final EventFilter eventFilter, final EventListener delegate)
  {
    super(context, eventFilter);
    this.delegate = delegate;
  }

  @Override
  protected void doOnEvent(final Event event) throws Exception
  {
    delegate.onEvent(event);
  }
}
