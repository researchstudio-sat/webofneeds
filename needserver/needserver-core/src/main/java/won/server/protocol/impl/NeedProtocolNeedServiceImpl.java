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

import won.protocol.exception.*;
import won.protocol.need.NeedProtocolNeedService;
import won.protocol.service.ConnectionCommunicationService;
import won.protocol.service.NeedFacingNeedCommunicationService;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class NeedProtocolNeedServiceImpl implements NeedProtocolNeedService
{
  private NeedFacingNeedCommunicationService needFacingNeedCommunicationService;
  private ConnectionCommunicationService connectionCommunicationService;

  @Override
  public URI connectionRequested(final URI need, final URI otherNeedURI, final URI otherConnectionURI, final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    return this.needFacingNeedCommunicationService.connectionRequested(need, otherNeedURI, otherConnectionURI, message);
  }

  @Override
  public void accept(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    connectionCommunicationService.accept(connectionURI);
  }

  @Override
  public void deny(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    connectionCommunicationService.deny(connectionURI);
  }

  @Override
  public void close(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    connectionCommunicationService.close(connectionURI);
  }

  @Override
  public void sendTextMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    connectionCommunicationService.sendTextMessage(connectionURI, message);
  }

  public void setNeedFacingNeedCommunicationService(final NeedFacingNeedCommunicationService needFacingNeedCommunicationService)
  {
    this.needFacingNeedCommunicationService = needFacingNeedCommunicationService;
  }

  public void setConnectionCommunicationService(final ConnectionCommunicationService connectionCommunicationService)
  {
    this.connectionCommunicationService = connectionCommunicationService;
  }
}
