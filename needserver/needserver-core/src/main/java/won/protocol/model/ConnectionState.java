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
 * Date: 30.10.12
 */
public enum ConnectionState
{
  REQUEST_SENT,
  REQUEST_RECEIVED,
  ESTABLISHED,
  CLOSED;

  public ConnectionState transit(ConnectionMessage msg){
    switch(this){
      case REQUEST_SENT: //the owner has initiated the connection, the request was sent to the remote need
        switch(msg){
           case PARTNER_ACCEPT: return ESTABLISHED;  //the partner accepted
           case PARTNER_DENY: return CLOSED;
          }
      case REQUEST_RECEIVED: //a remote need has requested a connection
        switch(msg){
          case OWNER_ACCEPT: return ESTABLISHED;
          case OWNER_DENY: return CLOSED;
        }
      case ESTABLISHED: //the connection is established
        switch(msg){
          case PARTNER_CLOSE: return CLOSED;
          case OWNER_CLOSE: return CLOSED;
        }
    }
    return this;
  }
}
