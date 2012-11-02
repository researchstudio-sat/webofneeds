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

package won.owner.protocol.local;

import com.hp.hpl.jena.graph.Graph;
import won.protocol.exception.*;
import won.protocol.model.Match;
import won.protocol.owner.NodeFromOwnerReceiver;
import won.protocol.owner.OwnerToNodeSender;

import java.net.URI;
import java.util.Collection;

/**
 * Implementation for testing purposes; communicates only with partners within the same VM.
 */
public class OwnerToNodeSenderLocalImpl implements OwnerToNodeSender
{
  private NodeFromOwnerReceiver receiver;

  @Override
  public URI sendCreateNeed(final URI ownerURI, final Graph content, final boolean activate) throws IllegalNeedContentException
  {
    return receiver.createNeed(ownerURI, content, activate);
  }

  @Override
  public void sendActivateNeed(final URI needURI) throws NoSuchNeedException
  {
    receiver.activate(needURI);
  }

  @Override
  public void sendDeactivateNeed(final URI needURI) throws NoSuchNeedException
  {
    receiver.deactivate(needURI);
  }

  @Override
  public URI sendConnectTo(final URI needURI, final URI otherNeedURI, final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    return receiver.connectTo(needURI, otherNeedURI, message);
  }

  @Override
  public void sendAccept(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    receiver.accept(connectionURI);
  }

  @Override
  public void sendDeny(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    receiver.deny(connectionURI);
  }

  @Override
  public void sendClose(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    receiver.close(connectionURI);

  }

  @Override
  public void sendMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    receiver.sendMessage(connectionURI, message);
  }

  @Override
  public Collection<URI> sendListNeedURIs()
  {
    return receiver.listNeedURIs();
  }

  @Override
  public Collection<Match> sendGetMatches(final URI needURI) throws NoSuchNeedException
  {
    return receiver.getMatches(needURI);
  }

  @Override
  public Collection<URI> sendListConnectionURIs(final URI needURI) throws NoSuchNeedException
  {
    return receiver.listConnectionURIs(needURI);
  }
}
