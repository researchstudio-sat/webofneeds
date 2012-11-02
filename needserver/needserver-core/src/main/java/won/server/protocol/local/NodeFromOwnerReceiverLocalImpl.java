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

import com.hp.hpl.jena.graph.Graph;
import won.protocol.exception.*;
import won.protocol.model.Match;
import won.protocol.owner.NodeFromOwnerReceiver;
import won.server.service.ConnectionService;
import won.server.service.NeedService;

import java.net.URI;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class NodeFromOwnerReceiverLocalImpl implements NodeFromOwnerReceiver
{
  private NeedService needService;
  private ConnectionService connectionService;

  @Override
  public URI createNeed(URI ownerURI, final Graph content, final boolean activate) throws IllegalNeedContentException
  {
    return this.needService.createNeed(ownerURI, content, activate);
  }

  @Override
  public void activate(final URI needURI) throws NoSuchNeedException
  {
    this.needService.activate(needURI);
  }

  @Override
  public void deactivate(final URI needURI) throws NoSuchNeedException
  {
    this.needService.deactivate(needURI);
  }

  @Override
  public URI connectTo(final URI need, final URI otherNeedURI, final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    return this.needService.connectTo(need,otherNeedURI,message);
  }

  @Override
  public void accept(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.connectionService.accept(connectionURI);
  }

  @Override
  public void deny(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.connectionService.deny(connectionURI);
  }

  @Override
  public void close(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.connectionService.close(connectionURI);
  }

  @Override
  public void sendMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.connectionService.sendMessage(connectionURI,message);
  }

  @Override
  public Collection<URI> listNeedURIs()
  {
    return this.needService.listNeedURIs();
  }

  @Override
  public Collection<Match> getMatches(final URI needURI) throws NoSuchNeedException
  {
    return this.needService.getMatches(needURI);
  }

  @Override
  public Collection<URI> listConnectionURIs(final URI needURI) throws NoSuchNeedException
  {
    return this.needService.listConnectionURIs(needURI);
  }

  public NeedService getNeedService()
  {
    return needService;
  }

  public void setNeedService(final NeedService needService)
  {
    this.needService = needService;
  }

  public ConnectionService getConnectionService()
  {
    return connectionService;
  }

  public void setConnectionService(final ConnectionService connectionService)
  {
    this.connectionService = connectionService;
  }
}
