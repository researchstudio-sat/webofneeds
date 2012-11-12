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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * User: fkleedorfer
 * Date: 06.11.12
 */
public class MockOwnerService extends AbstractOwnerProtocolOwnerService
{
  private static int messageCount = 0;
  private boolean autoConnect = true;
  private ConnectionAction onConnectAction;
  private ConnectionAction onAcceptAction;
  private ConnectionAction onMessageAction;
  private ConnectionAction onDenyAction;
  private ConnectionAction onCloseAction;
  private Map<Method,Integer> methodCallCounts;

  public enum ConnectionAction {ACCEPT, DENY, MESSAGE, CLOSE, NONE};
  public enum Method  {hintReceived,connectionRequested,accept,deny,close,sendTextMessage,EXCEPTION_CAUGHT};

  private CountDownLatch automaticActionsFinished;


  public MockOwnerService()
  {
    reset();
  }


  @Override
  public URI connectTo(final URI needURI, final URI otherNeedURI, final String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    try {
      return super.connectTo(needURI, otherNeedURI, message);
    }  catch (WonProtocolException wpe) {
      countMethodCall(Method.EXCEPTION_CAUGHT);
      System.out.println("caught exception in automatic owner after executing connectTo:");
      wpe.printStackTrace();
      if (this.automaticActionsFinished != null){
        this.automaticActionsFinished.countDown();
      }
      throw wpe;
    }
  }

  @Override
  public void hintReceived(final URI ownNeedURI, final URI otherNeedURI, final double score, final URI originatorURI) throws NoSuchNeedException
  {
    countMethodCall(Method.hintReceived);
    if (autoConnect) {
      this.lastConnectionURI = this.ownerProtocolNeedService.connectTo(ownNeedURI,otherNeedURI,"I'm automatically interested, take " + (messageCount++) );
    }
  }

  @Override
  public void connectionRequested(final URI ownNeedURI, final URI otherNeedURI, final URI ownConnectionURI, final String message) throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException
  {
    countMethodCall(Method.connectionRequested);
    this.lastConnectionURI = ownConnectionURI;
    performAction(onConnectAction, ownConnectionURI);
  }

  @Override
  public void accept(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    countMethodCall(Method.accept);
    performAction(onAcceptAction, connectionURI);
  }

  @Override
  public void deny(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    countMethodCall(Method.deny);
    performAction(onDenyAction, connectionURI);
  }

  @Override
  public void close(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    countMethodCall(Method.close);
    performAction(onCloseAction, connectionURI);
  }

  @Override
  public void sendTextMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    countMethodCall(Method.sendTextMessage);
    performAction(onMessageAction, connectionURI);
  }

  public int getMethodCallCount(Method m){
    Integer count = this.methodCallCounts.get(m);
    if (count == null) return 0;
    return count;
  }

  public void reset(){
    this.messageCount = 0;
    this.autoConnect = false;
    this.onConnectAction = ConnectionAction.NONE;
    this.onAcceptAction = ConnectionAction.NONE;
    this.onMessageAction = ConnectionAction.NONE;
    this.onDenyAction = ConnectionAction.NONE;
    this.onCloseAction = ConnectionAction.NONE;
    this.methodCallCounts = new HashMap<Method,Integer>();
    this.lastConnectionURI = null;
  }

  private synchronized void countMethodCall(Method m){
    Integer count = this.methodCallCounts.get(m);
    if (count == null) count = 0;
    this.methodCallCounts.put(m,count+1);
  }

  public void performAction(ConnectionAction action, URI connectionURI) {
    try{
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
        case NONE:
        default:
          if (this.automaticActionsFinished != null){
            this.automaticActionsFinished.countDown();
          }
          break;
      }
    } catch (WonProtocolException wpe) {
      countMethodCall(Method.EXCEPTION_CAUGHT);
      System.out.println("caught exception in automatic owner after executing " + action.name() + " action:");
      wpe.printStackTrace();
      if (this.automaticActionsFinished != null){
        this.automaticActionsFinished.countDown();
      }
    }
  }

  public void setAutomaticActionsFinished(final CountDownLatch automaticActionsFinished)
  {
    this.automaticActionsFinished = automaticActionsFinished;
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

  public URI getLastConnectionURI()
  {
    return lastConnectionURI;
  }
}
