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

import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * User: fkleedorfer Date: 02.11.12
 */
public enum ConnectionEventType {
  // in general, be permissive about messages where possible. Don't care about
  // duplicate messages

  // close may always be called. It always closes the connnection.
  OWNER_CLOSE("OwnerClose", ConnectionState.SUGGESTED, ConnectionState.REQUEST_SENT, ConnectionState.REQUEST_RECEIVED,
      ConnectionState.CONNECTED, ConnectionState.CLOSED),
  PARTNER_CLOSE("PartnerClose", ConnectionState.SUGGESTED, ConnectionState.REQUEST_SENT,
      ConnectionState.REQUEST_RECEIVED, ConnectionState.CONNECTED, ConnectionState.CLOSED),
  // open may always be called. It may re-open a connection, it does not change
  // the state of connected connections
  OWNER_OPEN("OwnerOpen", ConnectionState.SUGGESTED, ConnectionState.REQUEST_SENT, ConnectionState.REQUEST_RECEIVED,
      ConnectionState.CONNECTED, ConnectionState.CLOSED),
  PARTNER_OPEN("PartnerOpen", ConnectionState.SUGGESTED, ConnectionState.REQUEST_SENT, ConnectionState.REQUEST_RECEIVED,
      ConnectionState.CONNECTED, ConnectionState.CLOSED),

  OWNER_MESSAGE("OwnerMessage", ConnectionState.CONNECTED),
  PARTNER_MESSAGE("PartnerMessage", ConnectionState.CONNECTED),

  MATCHER_HINT("Hint");

  private String name;
  private ConnectionState[] permittingStates;

  ConnectionEventType(String name, ConnectionState... permittingStates) {
    this.permittingStates = permittingStates;
    this.name = name;
  }

  public boolean isMessageAllowed(ConnectionState stateToCheck) {
    for (ConnectionState st : this.permittingStates) {
      if (st.equals(stateToCheck))
        return true;
    }
    return false;
  }

  public URI getURI() {
    return URI.create(WON.BASE_URI + name);
  }
}
