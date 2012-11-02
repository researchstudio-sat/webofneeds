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
  OWNER_ACCEPT(ConnectionState.REQUEST_RECEIVED),
  OWNER_DENY(ConnectionState.REQUEST_RECEIVED),
  OWNER_CLOSE(ConnectionState.ESTABLISHED),
  OWNER_MESSAGE(ConnectionState.ESTABLISHED),
  PARTNER_ACCEPT(ConnectionState.REQUEST_SENT),
  PARTNER_DENY(ConnectionState.REQUEST_SENT),
  PARTNER_CLOSE(ConnectionState.ESTABLISHED),
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
