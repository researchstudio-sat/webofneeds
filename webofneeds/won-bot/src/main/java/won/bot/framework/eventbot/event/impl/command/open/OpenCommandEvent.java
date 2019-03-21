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

package won.bot.framework.eventbot.event.impl.command.open;

import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.protocol.message.WonMessageType;
import won.protocol.model.Connection;

import java.net.URI;

/**
 * Instructs the bot to open the specified connection behalf of the need.
 */
public class OpenCommandEvent extends BaseNeedAndConnectionSpecificEvent implements MessageCommandEvent {
  private String welcomeMessage;

  public OpenCommandEvent(Connection con, String welcomeMessage) {
    super(con);
    this.welcomeMessage = welcomeMessage;
  }

  public OpenCommandEvent(Connection con) {
    this(con, "Hello!");
  }

  public OpenCommandEvent(URI needURI, URI remoteNeedURI, URI connectionURI, String welcomeMessage) {
    this(makeConnection(needURI, remoteNeedURI, connectionURI), welcomeMessage);
  }

  public OpenCommandEvent(URI needURI, URI remoteNeedURI, URI connectionURI) {
    this(needURI, remoteNeedURI, connectionURI, "Hello!");
  }

  @Override public WonMessageType getWonMessageType() {
    return WonMessageType.OPEN;
  }

  public String getWelcomeMessage() {
    return welcomeMessage;
  }

}