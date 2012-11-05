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
import won.protocol.need.NeedProtocolService;
import won.protocol.service.ConnectionCommunicationService;
import won.protocol.service.NeedFacingNeedCommunicationService;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class NeedProtocolServiceImpl implements NeedProtocolService
{
  private NeedFacingNeedCommunicationService needCommunicationService;
  private ConnectionCommunicationService connectionCommunicationService;

  @Override
  public void connectionRequested(final URI need, final URI otherNeedURI, final URI otherConnectionURI, final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    this.needCommunicationService.connectionRequested(need, otherNeedURI, otherConnectionURI, message);
  }

  @Override
  public void connectionAccepted(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.connectionCommunicationService.accept(connectionURI);
  }

  @Override
  public void connectionDenied(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.connectionCommunicationService.deny(connectionURI);
  }

  @Override
  public void connectionClosed(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.connectionCommunicationService.close(connectionURI);
  }

  @Override
  public void textMessageReceived(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    this.connectionCommunicationService.sendTextMessage(connectionURI, message);
  }

  public void setNeedCommunicationService(final NeedFacingNeedCommunicationService needCommunicationService)
  {
    this.needCommunicationService = needCommunicationService;
  }

  public void setConnectionCommunicationService(final ConnectionCommunicationService connectionCommunicationService)
  {
    this.connectionCommunicationService = connectionCommunicationService;
  }
}
