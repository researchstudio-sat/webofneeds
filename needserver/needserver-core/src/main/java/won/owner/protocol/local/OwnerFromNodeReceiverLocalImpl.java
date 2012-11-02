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

import won.protocol.exception.*;
import won.protocol.owner.OwnerFromNodeReceiver;
import won.protocol.owner.OwnerToNodeSender;

import java.net.URI;

/**
 * Implementation for testing purposes; communicates only with partners within the same VM.
 */
public class OwnerFromNodeReceiverLocalImpl implements OwnerFromNodeReceiver
{
  protected OwnerToNodeSender sender;

  @Override
  public void hintReceived(final URI ownNeedURI, final URI otherNeedURI, final double score, final URI originatorURI) throws NoSuchNeedException
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void connectionRequested(final URI ownNeedURI, final URI otherNeedURI, final URI ownConnectionURI) throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void connectionAccepted(final URI ownConnectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void connectionDenied(final URI ownConnectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void connectionClosed(final URI ownConnectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void messageReceived(final URI ownConnectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void setSender(final OwnerToNodeSender sender)
  {
    this.sender = sender;
  }
}
