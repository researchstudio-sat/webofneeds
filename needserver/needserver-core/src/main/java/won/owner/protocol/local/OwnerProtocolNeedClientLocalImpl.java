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
import won.protocol.owner.OwnerProtocolNeedService;

import java.net.URI;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 05.11.12
 */
public class OwnerProtocolNeedClientLocalImpl implements OwnerProtocolNeedService
{

  private OwnerProtocolNeedService needService;

  @Override
  public URI createNeed(final URI ownerURI, final Graph content, final boolean activate) throws IllegalNeedContentException
  {
    return needService.createNeed(ownerURI, content, activate);
  }

  @Override
  public void activate(final URI needURI) throws NoSuchNeedException
  {
    needService.activate(needURI);
  }

  @Override
  public void deactivate(final URI needURI) throws NoSuchNeedException
  {
    needService.deactivate(needURI);
  }

  @Override
  public Collection<Match> getMatches(final URI needURI) throws NoSuchNeedException
  {
    return needService.getMatches(needURI);
  }

  @Override
  public Collection<URI> listNeedURIs()
  {
    return needService.listNeedURIs();
  }

  @Override
  public Collection<URI> listConnectionURIs(final URI needURI) throws NoSuchNeedException
  {
    return needService.listConnectionURIs(needURI);
  }

  @Override
  public void accept(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    needService.accept(connectionURI);
  }

  @Override
  public void deny(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    needService.deny(connectionURI);
  }

  @Override
  public void close(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    needService.close(connectionURI);
  }

  @Override
  public void sendTextMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    needService.sendTextMessage(connectionURI, message);
  }

  @Override
  public URI connectTo(final URI needURI, final URI otherNeedURI, final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    return needService.connectTo(needURI, otherNeedURI, message);
  }
}
