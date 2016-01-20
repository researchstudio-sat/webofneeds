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

package won.bot.framework.events.event.impl;

import won.bot.framework.events.event.BaseEvent;

/**
 * Indicates that something worked. What exactly that
 * was is indicated by the specified object.
 */
public class SuccessEvent extends BaseEvent {
  Object identifier;

  public SuccessEvent() {
    this.identifier = hashCode();
  }

  public SuccessEvent(Object identifier) {
    this.identifier = identifier;
  }

  public Object getIdentifier() {
    return identifier;
  }
}
