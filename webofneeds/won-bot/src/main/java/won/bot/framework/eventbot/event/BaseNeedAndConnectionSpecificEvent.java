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

package won.bot.framework.eventbot.event;

import java.net.URI;

import won.protocol.model.Connection;

/**
 *
 */
public abstract class BaseNeedAndConnectionSpecificEvent extends BaseEvent implements NeedSpecificEvent,
  ConnectionSpecificEvent, RemoteNeedSpecificEvent
{
  private final Connection con;

  public BaseNeedAndConnectionSpecificEvent(final Connection con)
  {
    this.con = con;
  }

  public Connection getCon()
  {
    return con;
  }

  @Override
  public URI getConnectionURI()
  {
    return con.getConnectionURI();
  }

  @Override
  public URI getNeedURI()
  {
    return con.getNeedURI();
  }

  @Override
  public URI getRemoteNeedURI() {
    return con.getRemoteNeedURI();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()+"@"+Integer.toHexString(hashCode())+ "{" +
      "needURI=" + getNeedURI() +
      ", connectionURI=" + getConnectionURI() +
      '}';
  }

  protected static Connection makeConnection(URI needURI, URI remoteNeedURI, URI connectionURI){
    Connection con = new Connection();
    con.setConnectionURI(connectionURI);
    con.setNeedURI(needURI);
    con.setRemoteNeedURI(remoteNeedURI);
    return con;
  }
}
