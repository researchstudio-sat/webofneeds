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

package won.bot.framework.eventbot.event.impl.wonmessage;

import won.bot.framework.eventbot.event.BaseEvent;
import won.bot.framework.eventbot.event.MessageEvent;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;

/**
 * Created by fkleedorfer on 14.06.2016.
 */
public class WonMessageSentEvent extends BaseEvent implements MessageEvent {
  private final WonMessage message;

  public WonMessageSentEvent(WonMessage message) {
    this.message = message;
  }

  @Override
  public WonMessage getWonMessage() {
    return this.message;
  }

  @Override
  public WonMessageType getWonMessageType() {
    return this.message.getMessageType();
  }
}
