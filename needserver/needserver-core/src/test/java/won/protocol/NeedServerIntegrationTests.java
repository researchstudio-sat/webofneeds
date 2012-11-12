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

package won.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import won.owner.service.impl.MockMatchingService;
import won.owner.service.impl.MockOwnerService;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.Need;
import won.protocol.model.NeedState;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * User: fkleedorfer
 * Date: 06.11.12
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/applicationContext.xml","/services.xml", "/additionalIntegrationTestServices.xml"})
@TransactionConfiguration(transactionManager="transactionManager", defaultRollback=false)
public class NeedServerIntegrationTests
{

  @Autowired
  @Qualifier("ownerProtocolOwnerClient")
  private MockOwnerService ownerProtocolOwnerClient;

  @Autowired
  @Qualifier("ownerProtocolOwnerClient2")
  private MockOwnerService ownerProtocolOwnerClient2;

  @Autowired
  private MockMatchingService mockMatchingService;

  @Test
  @Transactional(propagation = Propagation.NEVER)
  public void testNeedCreation(){
    URI ownerURI = createOwnerURI();
    URI matcherURI = createMatcherURI();

    URI need1URI = ownerProtocolOwnerClient.createNeed(ownerURI,null,false);
    URI need2URI = ownerProtocolOwnerClient2.createNeed(ownerURI,null,false);
    Assert.assertNotSame("Two consecutively created needs have the same URI", need1URI, need2URI);

    //try to connect the needs (both are inactive)
    try {
      URI connectionURI = ownerProtocolOwnerClient.connectTo(need1URI,need2URI,"If this connection is created, there's something wrong!");
      Assert.fail("Expected exception not thrown");
    } catch (IllegalMessageForNeedStateException e){
      Assert.assertEquals(need1URI,e.getNeedURI());
      Assert.assertEquals("CONNECT_TO",e.getMethodName());
      Assert.assertEquals(NeedState.INACTIVE,e.getNeedState());
    }

    //try that from the other need, too
    try {
      URI connectionURI = ownerProtocolOwnerClient2.connectTo(need2URI,need1URI,"If this connection is created, there's something wrong!");
      Assert.fail("Expected exception not thrown");
    } catch (IllegalMessageForNeedStateException e){
      Assert.assertEquals(need2URI,e.getNeedURI());
      Assert.assertEquals("CONNECT_TO",e.getMethodName());
      Assert.assertEquals(NeedState.INACTIVE,e.getNeedState());
    }

    //try to give a hint
    try {
      mockMatchingService.hint(need1URI,need2URI,1.0);
      Assert.fail("Expected exception not thrown");
    } catch (IllegalMessageForNeedStateException e){
      Assert.assertEquals(need1URI,e.getNeedURI());
      Assert.assertEquals("HINT",e.getMethodName());
      Assert.assertEquals(NeedState.INACTIVE,e.getNeedState());
    }


  }

  @Test
  //Propagation.NEVER is required here because if transactions start here and propagate,
  //code running in different threads won't inherit the transaction, and won't see changes to the database
  //made within the transaction
  @Transactional(propagation = Propagation.NEVER)
  public void testNormalConversation() throws Exception{
    URI ownerURI = createOwnerURI();
    URI matcherURI = createMatcherURI();
    CountDownLatch countDownLatch = new CountDownLatch(1); //used to synchronize with the owners' Threads

    ownerProtocolOwnerClient.reset();
    ownerProtocolOwnerClient.setAutoConnect(true);
    ownerProtocolOwnerClient.setOnAcceptAction("MESSAGE");
    ownerProtocolOwnerClient.setAutomaticActionsFinished(countDownLatch);

    ownerProtocolOwnerClient2.reset();
    ownerProtocolOwnerClient2.setOnConnectAction("ACCEPT");
    ownerProtocolOwnerClient2.setOnMessageAction("CLOSE");
    ownerProtocolOwnerClient2.setAutomaticActionsFinished(countDownLatch);

    URI needURI = ownerProtocolOwnerClient.createNeed(ownerURI,null,true);
    URI needURI2 = ownerProtocolOwnerClient2.createNeed(ownerURI,null,true);
    Assert.assertNotSame("Two consecutively created needs have the same URI", needURI, needURI2);

    mockMatchingService.hint(needURI,needURI2,0.9); //causes ownwer1 to connect (as it is on auto-connect)
    countDownLatch.await(10, TimeUnit.SECONDS);


    URI connectionURI = ownerProtocolOwnerClient.getLastConnectionURI();
    URI connectionURI2 = ownerProtocolOwnerClient2.getLastConnectionURI();
    Assert.assertNotNull(connectionURI);
    Assert.assertNotNull(connectionURI2);


    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.close));

    //now check both need and connection states
    Collection<URI> needConnections = ownerProtocolOwnerClient.listConnectionURIs(needURI);
    Assert.assertEquals(1, needConnections.size());
    //Assert.assertTrue(needConnections.contains(connectionURI));
    Connection conn = ownerProtocolOwnerClient.readConnection(connectionURI);
    Assert.assertEquals(connectionURI, conn.getConnectionURI());
    Assert.assertEquals(connectionURI2,conn.getRemoteConnectionURI());
    Assert.assertEquals(needURI,conn.getNeedURI());
    Assert.assertEquals(needURI2,conn.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.CLOSED,conn.getState());

    Collection<URI> needConnections2 = ownerProtocolOwnerClient2.listConnectionURIs(needURI2);
    Assert.assertEquals(1, needConnections2.size());
    Assert.assertTrue(needConnections2.contains(connectionURI2));
    Connection conn2 = ownerProtocolOwnerClient2.readConnection(connectionURI2);
    Assert.assertEquals(connectionURI2,conn2.getConnectionURI());
    Assert.assertEquals(connectionURI,conn2.getRemoteConnectionURI());
    Assert.assertEquals(needURI2,conn2.getNeedURI());
    Assert.assertEquals(needURI,conn2.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.CLOSED,conn2.getState());
  }

  @Test
  //Propagation.NEVER is required here because if transactions start here and propagate,
  //code running in different threads won't inherit the transaction, and won't see changes to the database
  //made within the transaction
  @Transactional(propagation = Propagation.NEVER)

  public void testDeniedConnection() throws Exception{
    URI ownerURI = createOwnerURI();
    URI matcherURI = createMatcherURI();
    CountDownLatch countDownLatch = new CountDownLatch(1); //used to synchronize with the owners' Threads

    ownerProtocolOwnerClient.reset();
    ownerProtocolOwnerClient.setAutoConnect(true);
    ownerProtocolOwnerClient.setAutomaticActionsFinished(countDownLatch);

    ownerProtocolOwnerClient2.reset();
    ownerProtocolOwnerClient2.setOnConnectAction("DENY");
    ownerProtocolOwnerClient2.setAutomaticActionsFinished(countDownLatch);

    URI needURI = ownerProtocolOwnerClient.createNeed(ownerURI,null,true);
    URI needURI2 = ownerProtocolOwnerClient2.createNeed(ownerURI,null,true);
    Assert.assertNotSame("Two consecutively created needs have the same URI", needURI, needURI2);

    mockMatchingService.hint(needURI,needURI2,0.9); //causes ownwer1 to connect (as it is on auto-connect)
    countDownLatch.await(10, TimeUnit.SECONDS);

    URI connectionURI = ownerProtocolOwnerClient.getLastConnectionURI();
    URI connectionURI2 = ownerProtocolOwnerClient2.getLastConnectionURI();
    Assert.assertNotNull(connectionURI);
    Assert.assertNotNull(connectionURI2);

    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));

    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));

    //now check both need and connection states
    Collection<URI> needConnections = ownerProtocolOwnerClient.listConnectionURIs(needURI);
    Assert.assertEquals(1, needConnections.size());
    //Assert.assertTrue(needConnections.contains(connectionURI));
    Connection conn = ownerProtocolOwnerClient.readConnection(connectionURI);
    Assert.assertEquals(connectionURI, conn.getConnectionURI());
    Assert.assertEquals(connectionURI2,conn.getRemoteConnectionURI());
    Assert.assertEquals(needURI,conn.getNeedURI());
    Assert.assertEquals(needURI2,conn.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.CLOSED,conn.getState());

    Collection<URI> needConnections2 = ownerProtocolOwnerClient2.listConnectionURIs(needURI2);
    Assert.assertEquals(1, needConnections2.size());
    Assert.assertTrue(needConnections2.contains(connectionURI2));
    Connection conn2 = ownerProtocolOwnerClient2.readConnection(connectionURI2);
    Assert.assertEquals(connectionURI2,conn2.getConnectionURI());
    Assert.assertEquals(connectionURI,conn2.getRemoteConnectionURI());
    Assert.assertEquals(needURI2,conn2.getNeedURI());
    Assert.assertEquals(needURI,conn2.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.CLOSED,conn2.getState());
  }

  @Test
  public void testAbortConnectionByDeactivate() throws Exception{
    URI ownerURI = createOwnerURI();
    URI matcherURI = createMatcherURI();
    CountDownLatch countDownLatch = new CountDownLatch(1); //used to synchronize with the owners' Threads

    ownerProtocolOwnerClient.reset();
    ownerProtocolOwnerClient.setAutoConnect(true);
    ownerProtocolOwnerClient.setAutomaticActionsFinished(countDownLatch);

    ownerProtocolOwnerClient2.reset();
    ownerProtocolOwnerClient2.setOnConnectAction("ACCEPT");
    ownerProtocolOwnerClient2.setAutomaticActionsFinished(countDownLatch);

    URI needURI = ownerProtocolOwnerClient.createNeed(ownerURI,null,true);
    URI needURI2 = ownerProtocolOwnerClient2.createNeed(ownerURI,null,true);
    Assert.assertNotSame("Two consecutively created needs have the same URI", needURI, needURI2);

    mockMatchingService.hint(needURI,needURI2,0.9); //causes ownwer1 to connect (as it is on auto-connect)

    countDownLatch.await(10, TimeUnit.SECONDS);

    URI connectionURI = ownerProtocolOwnerClient.getLastConnectionURI();
    URI connectionURI2 = ownerProtocolOwnerClient2.getLastConnectionURI();
    Assert.assertNotNull(connectionURI);
    Assert.assertNotNull(connectionURI2);


    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));

    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));

    //now check both need and connection states
    Collection<URI> needConnections = ownerProtocolOwnerClient.listConnectionURIs(needURI);
    Assert.assertEquals(1, needConnections.size());
    //Assert.assertTrue(needConnections.contains(connectionURI));
    Connection conn = ownerProtocolOwnerClient.readConnection(connectionURI);
    Assert.assertEquals(connectionURI, conn.getConnectionURI());
    Assert.assertEquals(connectionURI2,conn.getRemoteConnectionURI());
    Assert.assertEquals(needURI,conn.getNeedURI());
    Assert.assertEquals(needURI2,conn.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.ESTABLISHED,conn.getState());

    Collection<URI> needConnections2 = ownerProtocolOwnerClient2.listConnectionURIs(needURI2);
    Assert.assertEquals(1, needConnections2.size());
    Assert.assertTrue(needConnections2.contains(connectionURI2));
    Connection conn2 = ownerProtocolOwnerClient2.readConnection(connectionURI2);
    Assert.assertEquals(connectionURI2,conn2.getConnectionURI());
    Assert.assertEquals(connectionURI,conn2.getRemoteConnectionURI());
    Assert.assertEquals(needURI2,conn2.getNeedURI());
    Assert.assertEquals(needURI,conn2.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.ESTABLISHED,conn2.getState());

    //now, owner2 deactivates the need:
    countDownLatch = new CountDownLatch(1); //used to synchronize with the owners' Threads
    ownerProtocolOwnerClient.setAutomaticActionsFinished(countDownLatch);
    ownerProtocolOwnerClient2.setAutomaticActionsFinished(countDownLatch);
    ownerProtocolOwnerClient2.deactivate(needURI);
    countDownLatch.await(10, TimeUnit.SECONDS);

    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));

    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));

    //now check both need and connection states
    needConnections = ownerProtocolOwnerClient.listConnectionURIs(needURI);
    Assert.assertEquals(1, needConnections.size());
    //Assert.assertTrue(needConnections.contains(connectionURI));
    conn = ownerProtocolOwnerClient.readConnection(connectionURI);
    Assert.assertEquals(connectionURI, conn.getConnectionURI());
    Assert.assertEquals(connectionURI2,conn.getRemoteConnectionURI());
    Assert.assertEquals(needURI,conn.getNeedURI());
    Assert.assertEquals(needURI2,conn.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.CLOSED,conn.getState());

    needConnections2 = ownerProtocolOwnerClient2.listConnectionURIs(needURI2);
    Assert.assertEquals(1, needConnections2.size());
    Assert.assertTrue(needConnections2.contains(connectionURI2));
    conn2 = ownerProtocolOwnerClient2.readConnection(connectionURI2);
    Assert.assertEquals(connectionURI2,conn2.getConnectionURI());
    Assert.assertEquals(connectionURI,conn2.getRemoteConnectionURI());
    Assert.assertEquals(needURI2,conn2.getNeedURI());
    Assert.assertEquals(needURI,conn2.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.CLOSED,conn2.getState());
    System.out.println(conn);
    System.out.println(conn2);

    //check that need1 is closed
    Need need = ownerProtocolOwnerClient.readNeed(needURI);
    Assert.assertEquals(needURI,need.getNeedURI());
    Assert.assertEquals(NeedState.INACTIVE, need.getState());

  }

  @Test
  public void testProtocolErrorThenNormal() throws Exception{
    URI ownerURI = createOwnerURI();
    URI matcherURI = createMatcherURI();
    CountDownLatch countDownLatch = new CountDownLatch(1); //used to synchronize with the owners' Threads

    ownerProtocolOwnerClient.reset();
    ownerProtocolOwnerClient.setAutoConnect(true);
    ownerProtocolOwnerClient.setAutomaticActionsFinished(countDownLatch);

    ownerProtocolOwnerClient2.reset();
    ownerProtocolOwnerClient2.setOnConnectAction("MESSAGE");
    ownerProtocolOwnerClient2.setAutomaticActionsFinished(countDownLatch);

    URI needURI = ownerProtocolOwnerClient.createNeed(ownerURI,null,true);
    URI needURI2 = ownerProtocolOwnerClient2.createNeed(ownerURI,null,true);
    Assert.assertNotSame("Two consecutively created needs have the same URI", needURI, needURI2);

    mockMatchingService.hint(needURI,needURI2,0.9); //causes ownwer1 to connect (as it is on auto-connect)

    countDownLatch.await(10, TimeUnit.SECONDS);

    URI connectionURI = ownerProtocolOwnerClient.getLastConnectionURI();
    URI connectionURI2 = ownerProtocolOwnerClient2.getLastConnectionURI();
    Assert.assertNotNull(connectionURI);
    Assert.assertNotNull(connectionURI2);


    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));

    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));

    //now check both need and connection states
    Collection<URI> needConnections = ownerProtocolOwnerClient.listConnectionURIs(needURI);
    Assert.assertEquals(1, needConnections.size());
    //Assert.assertTrue(needConnections.contains(connectionURI));
    Connection conn = ownerProtocolOwnerClient.readConnection(connectionURI);
    Assert.assertEquals(connectionURI, conn.getConnectionURI());
    Assert.assertEquals(connectionURI2,conn.getRemoteConnectionURI());
    Assert.assertEquals(needURI,conn.getNeedURI());
    Assert.assertEquals(needURI2,conn.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.REQUEST_SENT,conn.getState());

    Collection<URI> needConnections2 = ownerProtocolOwnerClient2.listConnectionURIs(needURI2);
    Assert.assertEquals(1, needConnections2.size());
    Assert.assertTrue(needConnections2.contains(connectionURI2));
    Connection conn2 = ownerProtocolOwnerClient2.readConnection(connectionURI2);
    Assert.assertEquals(connectionURI2,conn2.getConnectionURI());
    Assert.assertEquals(connectionURI,conn2.getRemoteConnectionURI());
    Assert.assertEquals(needURI2,conn2.getNeedURI());
    Assert.assertEquals(needURI,conn2.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.REQUEST_RECEIVED,conn2.getState());

    //now, send the right message from owner2 (accept)
    ownerProtocolOwnerClient.setOnAcceptAction("MESSAGE");
    ownerProtocolOwnerClient2.setOnMessageAction("MESSAGE");
    ownerProtocolOwnerClient.setOnMessageAction("CLOSE");
    countDownLatch = new CountDownLatch(1); //used to synchronize with the owners' Threads
    ownerProtocolOwnerClient.setAutomaticActionsFinished(countDownLatch);
    ownerProtocolOwnerClient2.setAutomaticActionsFinished(countDownLatch);

    ownerProtocolOwnerClient2.performAction(MockOwnerService.ConnectionAction.ACCEPT, connectionURI2);
    countDownLatch.await(10, TimeUnit.SECONDS);

    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));

    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));

    //now check both need and connection states
    needConnections = ownerProtocolOwnerClient.listConnectionURIs(needURI);
    Assert.assertEquals(1, needConnections.size());
    //Assert.assertTrue(needConnections.contains(connectionURI));
    Connection connAfter = ownerProtocolOwnerClient.readConnection(connectionURI);
    Assert.assertEquals(connectionURI, connAfter.getConnectionURI());
    Assert.assertEquals(connectionURI2,connAfter.getRemoteConnectionURI());
    Assert.assertEquals(needURI,connAfter.getNeedURI());
    Assert.assertEquals(needURI2,connAfter.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.CLOSED,connAfter.getState());

    needConnections2 = ownerProtocolOwnerClient2.listConnectionURIs(needURI2);
    Assert.assertEquals(1, needConnections2.size());
    Assert.assertTrue(needConnections2.contains(connectionURI2));
    Connection connAfter2 = ownerProtocolOwnerClient2.readConnection(connectionURI2);
    Assert.assertEquals(connectionURI2,connAfter2.getConnectionURI());
    Assert.assertEquals(connectionURI,connAfter2.getRemoteConnectionURI());
    Assert.assertEquals(needURI2,connAfter2.getNeedURI());
    Assert.assertEquals(needURI,connAfter2.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.CLOSED,connAfter2.getState());

    System.out.println("conn:" + connAfter);
    System.out.println("conn2:" + connAfter2);
  }

  @Test
  public void testConnectToInactiveNeed() throws Exception{
    URI ownerURI = createOwnerURI();
    URI matcherURI = createMatcherURI();
    CountDownLatch countDownLatch = new CountDownLatch(1); //used to synchronize with the owners' Threads

    ownerProtocolOwnerClient.reset();
    ownerProtocolOwnerClient.setAutoConnect(true);
    ownerProtocolOwnerClient.setAutomaticActionsFinished(countDownLatch);

    ownerProtocolOwnerClient2.reset();
    ownerProtocolOwnerClient2.setAutomaticActionsFinished(countDownLatch);

    URI needURI = ownerProtocolOwnerClient.createNeed(ownerURI,null,true);
    URI needURI2 = ownerProtocolOwnerClient2.createNeed(ownerURI,null,false); //leave need inactive!
    Assert.assertNotSame("Two consecutively created needs have the same URI", needURI, needURI2);

    mockMatchingService.hint(needURI,needURI2,0.9); //causes ownwer1 to connect (as it is on auto-connect)

    countDownLatch.await(100, TimeUnit.SECONDS);

    URI connectionURI = ownerProtocolOwnerClient.getLastConnectionURI();
    URI connectionURI2 = ownerProtocolOwnerClient2.getLastConnectionURI();
    Assert.assertNotNull(connectionURI);
    Assert.assertNull(connectionURI2);


    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));

    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));

    //now check both need and connection states
    Collection<URI> needConnections = ownerProtocolOwnerClient.listConnectionURIs(needURI);
    Assert.assertEquals(1, needConnections.size());
    //Assert.assertTrue(needConnections.contains(connectionURI));
    Connection conn = ownerProtocolOwnerClient.readConnection(connectionURI);
    Assert.assertEquals(connectionURI, conn.getConnectionURI());
    Assert.assertEquals(connectionURI2,conn.getRemoteConnectionURI());
    Assert.assertEquals(needURI,conn.getNeedURI());
    Assert.assertEquals(needURI2,conn.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.CLOSED,conn.getState());

    Collection<URI> needConnections2 = ownerProtocolOwnerClient2.listConnectionURIs(needURI2);
    Assert.assertEquals(0, needConnections2.size());
  }


  @Test
  //Propagation.NEVER is required here because if transactions start here and propagate,
  //code running in different threads won't inherit the transaction, and won't see changes to the database
  //made within the transaction
  public void testConnectTwice() throws Exception{
    URI ownerURI = createOwnerURI();
    URI matcherURI = createMatcherURI();


    ownerProtocolOwnerClient.reset();
    ownerProtocolOwnerClient2.reset();
    ownerProtocolOwnerClient2.setOnConnectAction("ACCEPT");
    CountDownLatch countDownLatch = resetCountDownLatchTo(1);

    URI needURI = ownerProtocolOwnerClient.createNeed(ownerURI,null,true);
    URI needURI2 = ownerProtocolOwnerClient2.createNeed(ownerURI,null,true);
    Assert.assertNotSame("Two consecutively created needs have the same URI", needURI, needURI2);

    URI connectionURI = ownerProtocolOwnerClient.connectTo(needURI,needURI2,"this is the first connection attempt");
    countDownLatch.await(10, TimeUnit.SECONDS);
    countDownLatch = resetCountDownLatchTo(1);
    try {
      connectionURI = ownerProtocolOwnerClient.connectTo(needURI,needURI2,"this is the second connection attempt");
      Assert.fail("exception expected");
    } catch (ConnectionAlreadyExistsException e) {
      Assert.assertEquals(connectionURI, e.getConnectionURI());
      Assert.assertEquals(needURI, e.getFromNeedURI());
      Assert.assertEquals(needURI2, e.getToNeedURI());
      //ignore
    } catch (Exception e) {
      Assert.fail("wrong exception was thrown:"+e);
    }
  }

  @Test
  //Propagation.NEVER is required here because if transactions start here and propagate,
  //code running in different threads won't inherit the transaction, and won't see changes to the database
  //made within the transaction
  public void testReconnect() throws Exception{
    URI ownerURI = createOwnerURI();
    URI matcherURI = createMatcherURI();


    ownerProtocolOwnerClient.reset();
    ownerProtocolOwnerClient2.reset();
    ownerProtocolOwnerClient.setOnAcceptAction("CLOSE");
    ownerProtocolOwnerClient2.setOnConnectAction("ACCEPT");

    URI needURI = ownerProtocolOwnerClient.createNeed(ownerURI,null,true);
    URI needURI2 = ownerProtocolOwnerClient2.createNeed(ownerURI,null,true);
    Assert.assertNotSame("Two consecutively created needs have the same URI", needURI, needURI2);
    CountDownLatch countDownLatch = resetCountDownLatchTo(1);

    ownerProtocolOwnerClient.connectTo(needURI,needURI2,"this is the first connection attempt");
    countDownLatch.await(10, TimeUnit.SECONDS);

    URI connectionURI = ownerProtocolOwnerClient.getLastConnectionURI();
    URI connectionURI2 = ownerProtocolOwnerClient2.getLastConnectionURI();
    Assert.assertNotNull(connectionURI);
    Assert.assertNotNull(connectionURI2);

    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));

    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));

    //now check both need and connection states
    Collection<URI> needConnections = ownerProtocolOwnerClient.listConnectionURIs(needURI);
    Assert.assertEquals(1, needConnections.size());
    //Assert.assertTrue(needConnections.contains(connectionURI));
    Connection conn = ownerProtocolOwnerClient.readConnection(connectionURI);
    Assert.assertEquals(connectionURI, conn.getConnectionURI());
    Assert.assertEquals(connectionURI2,conn.getRemoteConnectionURI());
    Assert.assertEquals(needURI,conn.getNeedURI());
    Assert.assertEquals(needURI2,conn.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.CLOSED,conn.getState());

    Collection<URI> needConnections2 = ownerProtocolOwnerClient2.listConnectionURIs(needURI2);
    Assert.assertEquals(1, needConnections2.size());
    Assert.assertTrue(needConnections2.contains(connectionURI2));
    Connection connAfter2 = ownerProtocolOwnerClient2.readConnection(connectionURI2);
    Assert.assertEquals(connectionURI2,connAfter2.getConnectionURI());
    Assert.assertEquals(connectionURI,connAfter2.getRemoteConnectionURI());
    Assert.assertEquals(needURI2,connAfter2.getNeedURI());
    Assert.assertEquals(needURI,connAfter2.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.CLOSED,connAfter2.getState());


    ownerProtocolOwnerClient.reset();
    ownerProtocolOwnerClient2.reset();
    ownerProtocolOwnerClient.setOnAcceptAction("CLOSE");
    ownerProtocolOwnerClient2.setOnConnectAction("ACCEPT");
    countDownLatch = resetCountDownLatchTo(1);
    connectionURI = ownerProtocolOwnerClient.connectTo(needURI,needURI2,"this is the first connection attempt");
    countDownLatch.await(10, TimeUnit.SECONDS);

    connectionURI = ownerProtocolOwnerClient.getLastConnectionURI();
    connectionURI2 = ownerProtocolOwnerClient2.getLastConnectionURI();
    Assert.assertNotNull(connectionURI);
    Assert.assertNotNull(connectionURI2);

    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));

    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));

    //now check both need and connection states
    needConnections = ownerProtocolOwnerClient.listConnectionURIs(needURI);
    Assert.assertEquals(2, needConnections.size());
    //Assert.assertTrue(needConnections.contains(connectionURI));
    conn = ownerProtocolOwnerClient.readConnection(connectionURI);
    Assert.assertEquals(connectionURI, conn.getConnectionURI());
    Assert.assertEquals(connectionURI2,conn.getRemoteConnectionURI());
    Assert.assertEquals(needURI,conn.getNeedURI());
    Assert.assertEquals(needURI2,conn.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.CLOSED,conn.getState());

    needConnections2 = ownerProtocolOwnerClient2.listConnectionURIs(needURI2);
    Assert.assertEquals(2, needConnections2.size());
    Assert.assertTrue(needConnections2.contains(connectionURI2));
    connAfter2 = ownerProtocolOwnerClient2.readConnection(connectionURI2);
    Assert.assertEquals(connectionURI2,connAfter2.getConnectionURI());
    Assert.assertEquals(connectionURI,connAfter2.getRemoteConnectionURI());
    Assert.assertEquals(needURI2,connAfter2.getNeedURI());
    Assert.assertEquals(needURI,connAfter2.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.CLOSED,connAfter2.getState());
  }

  @Test
  public void testConnectFromBothSidesBeforeFirstAccept() throws Exception{
    URI ownerURI = createOwnerURI();
    URI matcherURI = createMatcherURI();

    ownerProtocolOwnerClient.reset();
    ownerProtocolOwnerClient2.reset();

    URI needURI = ownerProtocolOwnerClient.createNeed(ownerURI,null,true);
    URI needURI2 = ownerProtocolOwnerClient2.createNeed(ownerURI,null,true);

    Assert.assertNotSame("Two consecutively created needs have the same URI", needURI, needURI2);
    CountDownLatch countDownLatch = resetCountDownLatchTo(1);

    ownerProtocolOwnerClient.connectTo(needURI,needURI2,"this is the first connection attempt");
    countDownLatch.await(10,TimeUnit.SECONDS);
    countDownLatch = resetCountDownLatchTo(1);
    URI connectionURI = ownerProtocolOwnerClient.getLastConnectionURI();
    try {
      ownerProtocolOwnerClient2.connectTo(needURI2,needURI,"this is the concurrent connection attempt");
      countDownLatch.await(10,TimeUnit.SECONDS);
    } catch (ConnectionAlreadyExistsException e) {
      Assert.assertEquals(needURI2,e.getFromNeedURI());
      Assert.assertEquals(needURI,e.getToNeedURI());
    }


    URI connectionURI2 = ownerProtocolOwnerClient2.getLastConnectionURI();

    Assert.assertNotNull(connectionURI);
    Assert.assertNotNull(connectionURI2);

    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.close));

    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.close));
 }

  @Test
  public void testConnectTwiceBeforeFirstAccept() throws Exception{
    URI ownerURI = createOwnerURI();
    URI matcherURI = createMatcherURI();

    ownerProtocolOwnerClient.reset();
    ownerProtocolOwnerClient2.reset();

    URI needURI = ownerProtocolOwnerClient.createNeed(ownerURI,null,true);
    URI needURI2 = ownerProtocolOwnerClient2.createNeed(ownerURI,null,true);

    Assert.assertNotSame("Two consecutively created needs have the same URI", needURI, needURI2);
    CountDownLatch countDownLatch = resetCountDownLatchTo(1);

    ownerProtocolOwnerClient.connectTo(needURI,needURI2,"this is the first connection attempt");
    countDownLatch.await(10,TimeUnit.SECONDS);
    URI connectionURI = ownerProtocolOwnerClient.getLastConnectionURI();

    countDownLatch = resetCountDownLatchTo(1);
    try {
      ownerProtocolOwnerClient.connectTo(needURI2,needURI,"this is the concurrent connection attempt");
      countDownLatch.await(10,TimeUnit.SECONDS);
    } catch (ConnectionAlreadyExistsException e) {
      Assert.assertEquals(needURI2,e.getFromNeedURI());
      Assert.assertEquals(needURI,e.getToNeedURI());
    }




    URI connectionURI2 = ownerProtocolOwnerClient2.getLastConnectionURI();

    Assert.assertNotNull(connectionURI);
    Assert.assertNotNull(connectionURI2);

    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));

    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));
  }

  @Test
  public void testConnectTwiceAfterFirstAccept() throws Exception{
    URI ownerURI = createOwnerURI();
    URI matcherURI = createMatcherURI();

    ownerProtocolOwnerClient.reset();
    ownerProtocolOwnerClient2.reset();
    ownerProtocolOwnerClient2.setOnConnectAction("ACCEPT");

    URI needURI = ownerProtocolOwnerClient.createNeed(ownerURI,null,true);
    URI needURI2 = ownerProtocolOwnerClient2.createNeed(ownerURI,null,true);

    Assert.assertNotSame("Two consecutively created needs have the same URI", needURI, needURI2);
    CountDownLatch countDownLatch = resetCountDownLatchTo(1);

    ownerProtocolOwnerClient.connectTo(needURI,needURI2,"this is the first connection attempt");
    countDownLatch.await(10,TimeUnit.SECONDS);
    URI connectionURI = ownerProtocolOwnerClient.getLastConnectionURI();

    countDownLatch = resetCountDownLatchTo(1);

    try {
      ownerProtocolOwnerClient.connectTo(needURI2,needURI,"this is the concurrent connection attempt");
    } catch (ConnectionAlreadyExistsException e){
      Assert.assertEquals(needURI2,e.getFromNeedURI());
      Assert.assertEquals(needURI,e.getToNeedURI());
    }

    countDownLatch.await(10,TimeUnit.SECONDS);

    URI connectionURI2 = ownerProtocolOwnerClient2.getLastConnectionURI();

    Assert.assertNotNull(connectionURI);
    Assert.assertNotNull(connectionURI2);

    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));


    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));


  }

  @Test
  public void testConnectFromBothSidesAfterFirstAccept() throws Exception{
    URI ownerURI = createOwnerURI();
    URI matcherURI = createMatcherURI();

    ownerProtocolOwnerClient.reset();
    ownerProtocolOwnerClient2.reset();
    ownerProtocolOwnerClient2.setOnConnectAction("ACCEPT");

    URI needURI = ownerProtocolOwnerClient.createNeed(ownerURI,null,true);
    URI needURI2 = ownerProtocolOwnerClient2.createNeed(ownerURI,null,true);

    Assert.assertNotSame("Two consecutively created needs have the same URI", needURI, needURI2);
    CountDownLatch countDownLatch = resetCountDownLatchTo(1);

    ownerProtocolOwnerClient.connectTo(needURI,needURI2,"this is the first connection attempt");
    countDownLatch.await(10,TimeUnit.SECONDS);
    URI connectionURI = ownerProtocolOwnerClient.getLastConnectionURI();

    countDownLatch = resetCountDownLatchTo(1);
    try {
      ownerProtocolOwnerClient2.connectTo(needURI2,needURI,"this is the concurrent connection attempt");
    } catch (ConnectionAlreadyExistsException e){
      Assert.assertEquals(needURI2,e.getFromNeedURI());
      Assert.assertEquals(needURI,e.getToNeedURI());
    }
    countDownLatch.await(10,TimeUnit.SECONDS);


    URI connectionURI2 = ownerProtocolOwnerClient2.getLastConnectionURI();

    Assert.assertNotNull(connectionURI);
    Assert.assertNotNull(connectionURI2);

    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(1, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient.getMethodCallCount(MockOwnerService.Method.close));

    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.hintReceived));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.connectionRequested));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.accept));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.deny));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.sendTextMessage));
    Assert.assertEquals(0, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.close));
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.EXCEPTION_CAUGHT));



  }


  private CountDownLatch resetCountDownLatchTo(int n)
  {
    final CountDownLatch countDownLatch;
    countDownLatch = new CountDownLatch(n); //we have 2 activities going on, and we want to wait for both
    ownerProtocolOwnerClient.setAutomaticActionsFinished(countDownLatch);
    ownerProtocolOwnerClient2.setAutomaticActionsFinished(countDownLatch);
    return countDownLatch;
  }


  private URI createOwnerURI(){
    return URI.create("http://owner.com/op");
  }

  private URI createMatcherURI(){
    return URI.create("http://matcher.com/mp");
  }
}
