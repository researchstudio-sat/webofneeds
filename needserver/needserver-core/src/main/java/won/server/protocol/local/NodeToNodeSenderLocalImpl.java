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
import won.protocol.need.NodeFromNodeReceiver;
import won.protocol.need.NodeToNodeSender;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class NodeToNodeSenderLocalImpl implements NodeToNodeSender
{
  private NodeFromNodeReceiver receiver;


  @Override
  public void sendConnectionRequested(final URI needURI, final URI otherNeedURI, final URI otherConnectionURI, final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    this.receiver.connectionRequested(needURI,otherNeedURI,otherConnectionURI,message);
  }

  @Override
  public void sendConnectionAccepted(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.receiver.connectionAccepted(connectionURI);
  }

  @Override
  public void sendConnectionDenied(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.receiver.connectionDenied(connectionURI);
  }

  @Override
  public void sendConnectionClosed(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.receiver.connectionClosed(connectionURI);
  }

  @Override
  public void sendTextMessageReceived(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.receiver.textMessageReceived(connectionURI, message);
  }

  public NodeFromNodeReceiver getReceiver()
  {
    return receiver;
  }

  public void setReceiver(final NodeFromNodeReceiver receiver)
  {
    this.receiver = receiver;
  }
}
