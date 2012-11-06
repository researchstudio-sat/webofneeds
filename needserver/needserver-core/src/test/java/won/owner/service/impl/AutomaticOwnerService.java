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

package won.owner.service.impl;

import won.protocol.exception.*;
import won.protocol.owner.OwnerProtocolNeedService;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 06.11.12
 */
public class AutomaticOwnerService extends AbstractOwnerProtocolOwnerService
{
  private static int messageCount = 0;
  private boolean autoConnect = true;
  private ConnectionAction onConnectAction = ConnectionAction.ACCEPT;
  private ConnectionAction onAcceptAction = ConnectionAction.MESSAGE;
  private ConnectionAction onMessageAction = ConnectionAction.CLOSE;
  private ConnectionAction onDenyAction = ConnectionAction.NONE;
  private ConnectionAction onCloseAction = ConnectionAction.NONE;

  public enum ConnectionAction {ACCEPT, DENY, MESSAGE, CLOSE,NONE};



  @Override
  public void hintReceived(final URI ownNeedURI, final URI otherNeedURI, final double score, final URI originatorURI) throws NoSuchNeedException
  {
    if (autoConnect) {
      this.ownerProtocolNeedService.connectTo(otherNeedURI,ownNeedURI,"I'm automatically interested, take " + (messageCount++) );
    }
  }

  @Override
  public void connectionRequested(final URI ownNeedURI, final URI otherNeedURI, final URI ownConnectionURI, final String message) throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException
  {
    performAutomaticAction(onConnectAction, ownConnectionURI);
  }

  @Override
  public void accept(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    performAutomaticAction(onAcceptAction, connectionURI);
  }

  @Override
  public void deny(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    performAutomaticAction(onDenyAction, connectionURI);
  }

  @Override
  public void close(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    performAutomaticAction(onCloseAction, connectionURI);
  }

  @Override
  public void sendTextMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    performAutomaticAction(onMessageAction, connectionURI);
  }

  private void performAutomaticAction(ConnectionAction action, URI connectionURI ) {
    switch (action){
      case ACCEPT:
        this.ownerProtocolNeedService.accept(connectionURI );
        break;
      case DENY:
        this.ownerProtocolNeedService.deny(connectionURI );
        break;
      case MESSAGE:
        this.ownerProtocolNeedService.sendTextMessage(connectionURI,"this is my automatic message #" + (messageCount++));
        break;
      case CLOSE:
        this.ownerProtocolNeedService.close(connectionURI);
        break;
    }
  }

  @Override
  public void setOwnerProtocolNeedService(final OwnerProtocolNeedService ownerProtocolNeedService)
  {
    super.setOwnerProtocolNeedService(ownerProtocolNeedService);    //To change body of overridden methods use File | Settings | File Templates.
  }

  public void setAutoConnect(final boolean autoConnect)
  {
    this.autoConnect = autoConnect;
  }

  public void setOnConnectAction(final String action)
  {
    this.onConnectAction = ConnectionAction.valueOf(action);
  }

  public void setOnAcceptAction(final String action)
  {
    this.onAcceptAction = ConnectionAction.valueOf(action);
  }

  public void setOnMessageAction(final String action)
  {
    this.onMessageAction  = ConnectionAction.valueOf(action);
  }

  public void setOnDenyAction(final String action)
  {
    this.onDenyAction  = ConnectionAction.valueOf(action);
  }

  public void setOnCloseAction(final String action)
  {
    this.onCloseAction  = ConnectionAction.valueOf(action);
  }
}
