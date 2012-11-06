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
import won.protocol.owner.OwnerProtocolOwnerService;

import java.net.URI;

/**
 * TODO: Empty owner client implementation to be replaced by WS client.
 */
public class OwnerProtocolOwnerClientEmptyImpl implements OwnerProtocolOwnerService
{
  @Override
  public void hintReceived(final URI ownNeedURI, final URI otherNeedURI, final double score, final URI originatorURI) throws NoSuchNeedException
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void connectionRequested(final URI ownNeedURI, final URI otherNeedURI, final URI ownConnectionURI, final String message) throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void accept(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void deny(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void close(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void sendTextMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
