/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.protocol.model;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public enum ConnectionMessage
{
  //in general, be permissive about messages where possible. Don't care about duplicate messages

  OWNER_ACCEPT(ConnectionState.REQUEST_RECEIVED,ConnectionState.ESTABLISHED),
  OWNER_DENY(ConnectionState.REQUEST_RECEIVED,ConnectionState.CLOSED),
  //close may always be called. It always closes the connnection.
  OWNER_CLOSE(ConnectionState.ESTABLISHED, ConnectionState.REQUEST_SENT, ConnectionState.REQUEST_RECEIVED, ConnectionState.CLOSED),
  OWNER_MESSAGE(ConnectionState.ESTABLISHED),
  PARTNER_ACCEPT(ConnectionState.REQUEST_SENT,ConnectionState.ESTABLISHED),
  PARTNER_DENY(ConnectionState.REQUEST_SENT,ConnectionState.CLOSED),
  //close may always be called. It always closes the connnection.
  PARTNER_CLOSE(ConnectionState.ESTABLISHED, ConnectionState.REQUEST_SENT, ConnectionState.REQUEST_RECEIVED, ConnectionState.CLOSED),
  PARTNER_MESSAGE(ConnectionState.ESTABLISHED);

  private ConnectionState[] permittingStates;

  ConnectionMessage(ConnectionState... permittingStates){
    this.permittingStates = permittingStates;
  }

  public boolean isMessageAllowed(ConnectionState stateToCheck){
    for (ConnectionState st: this.permittingStates) {
      if (st.equals(stateToCheck)) return true;
    }
    return false;
  }
}
