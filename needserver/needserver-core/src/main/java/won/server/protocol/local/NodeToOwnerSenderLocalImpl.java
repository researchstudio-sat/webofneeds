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

package won.server.protocol.local;

import won.protocol.exception.*;
import won.protocol.owner.NodeToOwnerSender;
import won.protocol.owner.OwnerFromNodeReceiver;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class NodeToOwnerSenderLocalImpl implements NodeToOwnerSender
{
  private OwnerFromNodeReceiver receiver;

  @Override
  public void sendHintReceived(final URI ownNeed, final URI otherNeed, final double score, final URI originator) throws NoSuchNeedException
  {
    this.receiver.hintReceived(ownNeed,otherNeed,score,originator);
  }

  @Override
  public void sendConnectionRequested(final URI ownNeedURI, final URI otherNeedURI, final URI ownConnectionURI) throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException
  {
    this.receiver.connectionRequested(ownNeedURI,otherNeedURI,ownConnectionURI);
  }

  @Override
  public void sendConnectionAccepted(final URI ownConnectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.receiver.connectionAccepted(ownConnectionURI);
  }

  @Override
  public void sendConnectionDenied(final URI ownConnectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.receiver.connectionDenied(ownConnectionURI);
  }

  @Override
  public void sendConnectionClosed(final URI ownConnectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.receiver.connectionClosed(ownConnectionURI);
  }

  @Override
  public void sendMessageReceived(final URI ownConnectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.receiver.messageReceived(ownConnectionURI, message);
  }
}
