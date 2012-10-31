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

package won.server.model;

/**
 * User: fkleedorfer
 * Date: 30.10.12
 */
public enum ConnectionState
{
  /*
    in the following,
    'initiator' denotes the connection object on the initiating side,
    'target' denotes the connection on the receiving side
   */
  NEW,                    /* any: connection has just been created */
  REQUEST_SENT,           /* initiator: connection request was sent to other need */
  REQUEST_RECEIVED,       /* target: connection request was received from other need, passing on to owner  */
  REQUEST_ACCEPTED,       /* target: connection request was accepted by owner               */
  ESTABLISHED,    /* target: connection accept was sent to initiating connection    */
                 /* initiator: connection accept was received, passing on to owner */
  CLOSING,        /* owner or other side sent close signal, passing on to owner */
  CLOSED         /* passed closing signal on to owner */
}
