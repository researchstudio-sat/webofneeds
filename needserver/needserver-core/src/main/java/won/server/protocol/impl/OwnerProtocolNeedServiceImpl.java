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

package won.server.protocol.impl;

import com.hp.hpl.jena.graph.Graph;
import won.protocol.exception.*;
import won.protocol.model.Connection;
import won.protocol.model.Match;
import won.protocol.model.Need;
import won.protocol.owner.OwnerProtocolNeedService;
import won.protocol.service.ConnectionCommunicationService;
import won.protocol.service.NeedInformationService;
import won.protocol.service.NeedManagementService;
import won.protocol.service.OwnerFacingNeedCommunicationService;

import java.net.URI;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class OwnerProtocolNeedServiceImpl implements OwnerProtocolNeedService
{
  private OwnerFacingNeedCommunicationService needCommunicationService;
  private ConnectionCommunicationService connectionCommunicationService;
  private NeedManagementService needManagementService;
  private NeedInformationService needInformationService;

  @Override
  public URI createNeed(URI ownerURI, final Graph content, final boolean activate) throws IllegalNeedContentException
  {
    return this.needManagementService.createNeed(ownerURI, content, activate);
  }

  @Override
  public void activate(final URI needURI) throws NoSuchNeedException
  {
    this.needManagementService.activate(needURI);
  }

  @Override
  public void deactivate(final URI needURI) throws NoSuchNeedException
  {
    this.needManagementService.deactivate(needURI);
  }

  @Override
  public URI connectTo(final URI need, final URI otherNeedURI, final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    return this.needCommunicationService.connectTo(need,otherNeedURI,message);
  }

  @Override
  public void accept(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.connectionCommunicationService.accept(connectionURI);
  }

  @Override
  public void deny(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.connectionCommunicationService.deny(connectionURI);
  }

  @Override
  public void close(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.connectionCommunicationService.close(connectionURI);
  }

  @Override
  public void sendTextMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.connectionCommunicationService.sendTextMessage(connectionURI, message);
  }

  @Override
  public Collection<URI> listNeedURIs()
  {
    return this.needInformationService.listNeedURIs();
  }

  @Override
  public Collection<Match> getMatches(final URI needURI) throws NoSuchNeedException
  {
    return this.needManagementService.getMatches(needURI);
  }

  @Override
  public Collection<URI> listConnectionURIs(final URI needURI) throws NoSuchNeedException
  {
    return this.needInformationService.listConnectionURIs(needURI);
  }

  @Override
  public Need readNeed(final URI needURI) throws NoSuchNeedException
  {
    return needInformationService.readNeed(needURI);
  }

  @Override
  public Graph readNeedContent(final URI needURI) throws NoSuchNeedException
  {
    return needInformationService.readNeedContent(needURI);
  }

  @Override
  public Connection readConnection(final URI connectionURI) throws NoSuchConnectionException
  {
    return needInformationService.readConnection(connectionURI);
  }

  @Override
  public Graph readConnectionContent(final URI connectionURI) throws NoSuchConnectionException
  {
    return needInformationService.readConnectionContent(connectionURI);
  }

  public void setNeedCommunicationService(final OwnerFacingNeedCommunicationService needCommunicationService)
  {
    this.needCommunicationService = needCommunicationService;
  }

  public void setConnectionCommunicationService(final ConnectionCommunicationService connectionCommunicationService)
  {
    this.connectionCommunicationService = connectionCommunicationService;
  }

  public void setNeedManagementService(final NeedManagementService needManagementService)
  {
    this.needManagementService = needManagementService;
  }

  public void setNeedInformationService(final NeedInformationService needInformationService)
  {
    this.needInformationService = needInformationService;
  }
}
