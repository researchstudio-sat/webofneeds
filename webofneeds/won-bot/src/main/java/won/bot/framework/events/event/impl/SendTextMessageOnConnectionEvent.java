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
import won.bot.framework.events.event.ConnectionSpecificEvent;
import won.protocol.model.Connection;

import java.net.URI;

/**
 * Event that can be used to send a text message on a specified connection.
 * Used in conjunction with a SendTextMessageOnConnectionAction.
 */
public class SendTextMessageOnConnectionEvent extends BaseEvent implements ConnectionSpecificEvent
{
  private Connection connection;
  private String textMessage;

  public SendTextMessageOnConnectionEvent(final Connection con, final String textMessage) {
    this.connection = con;
    this.textMessage = textMessage;
  }

  @Override
  public URI getConnectionURI() {
    return connection.getConnectionURI();
  }

  @Override
  public URI getRemoteNeedURI() {
    return connection.getRemoteNeedURI();
  }

  public String getTextMessage() {
    return textMessage;
  }
}
