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
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.Need;
import won.protocol.model.NeedState;

import java.net.URI;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 06.11.12
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/applicationContext.xml","/services.xml", "/additionalIntegrationTestServices.xml"})
@TransactionConfiguration(transactionManager="transactionManager", defaultRollback=false)
@Transactional
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


    ownerProtocolOwnerClient.reset();
    ownerProtocolOwnerClient.setAutoConnect(true);
    ownerProtocolOwnerClient.setOnAcceptAction("MESSAGE");

    ownerProtocolOwnerClient2.reset();
    ownerProtocolOwnerClient2.setOnConnectAction("ACCEPT");
    ownerProtocolOwnerClient2.setOnMessageAction("CLOSE");

    URI needURI = ownerProtocolOwnerClient.createNeed(ownerURI,null,true);
    URI needURI2 = ownerProtocolOwnerClient2.createNeed(ownerURI,null,true);
    Assert.assertNotSame("Two consecutively created needs have the same URI", needURI, needURI2);

    mockMatchingService.hint(needURI,needURI2,0.9); //causes ownwer1 to connect (as it is on auto-connect)

    Thread.sleep(500);  //wait for all communication to play out

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
    Connection conn2 = ownerProtocolOwnerClient.readConnection(connectionURI2);
    Assert.assertEquals(connectionURI2,conn2.getConnectionURI());
    Assert.assertEquals(connectionURI,conn2.getRemoteConnectionURI());
    Assert.assertEquals(needURI2,conn2.getNeedURI());
    Assert.assertEquals(needURI,conn2.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.CLOSED,conn.getState());
  }

  @Test
  //Propagation.NEVER is required here because if transactions start here and propagate,
  //code running in different threads won't inherit the transaction, and won't see changes to the database
  //made within the transaction
  @Transactional(propagation = Propagation.NEVER)

  public void testDeniedConnection() throws Exception{
    URI ownerURI = createOwnerURI();
    URI matcherURI = createMatcherURI();


    ownerProtocolOwnerClient.reset();
    ownerProtocolOwnerClient.setAutoConnect(true);

    ownerProtocolOwnerClient2.reset();
    ownerProtocolOwnerClient2.setOnConnectAction("DENY");

    URI needURI = ownerProtocolOwnerClient.createNeed(ownerURI,null,true);
    URI needURI2 = ownerProtocolOwnerClient2.createNeed(ownerURI,null,true);
    Assert.assertNotSame("Two consecutively created needs have the same URI", needURI, needURI2);

    mockMatchingService.hint(needURI,needURI2,0.9); //causes ownwer1 to connect (as it is on auto-connect)

    Thread.sleep(500);  //wait for all communication to play out

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
    Connection conn2 = ownerProtocolOwnerClient.readConnection(connectionURI2);
    Assert.assertEquals(connectionURI2,conn2.getConnectionURI());
    Assert.assertEquals(connectionURI,conn2.getRemoteConnectionURI());
    Assert.assertEquals(needURI2,conn2.getNeedURI());
    Assert.assertEquals(needURI,conn2.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.CLOSED,conn.getState());
  }

  @Test
  //Propagation.NEVER is required here because if transactions start here and propagate,
  //code running in different threads won't inherit the transaction, and won't see changes to the database
  //made within the transaction
  @Transactional(propagation = Propagation.NEVER)
  public void testAbortConnectionByDeactivate() throws Exception{
    URI ownerURI = createOwnerURI();
    URI matcherURI = createMatcherURI();


    ownerProtocolOwnerClient.reset();
    ownerProtocolOwnerClient.setAutoConnect(true);

    ownerProtocolOwnerClient2.reset();
    ownerProtocolOwnerClient2.setOnConnectAction("ACCEPT");

    URI needURI = ownerProtocolOwnerClient.createNeed(ownerURI,null,true);
    URI needURI2 = ownerProtocolOwnerClient2.createNeed(ownerURI,null,true);
    Assert.assertNotSame("Two consecutively created needs have the same URI", needURI, needURI2);

    mockMatchingService.hint(needURI,needURI2,0.9); //causes ownwer1 to connect (as it is on auto-connect)

    Thread.sleep(500);  //wait for all communication to play out

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
    Connection conn2 = ownerProtocolOwnerClient.readConnection(connectionURI2);
    Assert.assertEquals(connectionURI2,conn2.getConnectionURI());
    Assert.assertEquals(connectionURI,conn2.getRemoteConnectionURI());
    Assert.assertEquals(needURI2,conn2.getNeedURI());
    Assert.assertEquals(needURI,conn2.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.ESTABLISHED,conn.getState());

    //now, owner1 deactivates the need:
    ownerProtocolOwnerClient.deactivate(needURI);
    Thread.sleep(500); //wait for communication to play out

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
    Assert.assertEquals(1, ownerProtocolOwnerClient2.getMethodCallCount(MockOwnerService.Method.close));
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
    conn2 = ownerProtocolOwnerClient.readConnection(connectionURI2);
    Assert.assertEquals(connectionURI2,conn2.getConnectionURI());
    Assert.assertEquals(connectionURI,conn2.getRemoteConnectionURI());
    Assert.assertEquals(needURI2,conn2.getNeedURI());
    Assert.assertEquals(needURI,conn2.getRemoteNeedURI());
    Assert.assertEquals(ConnectionState.CLOSED,conn.getState());

    //check that need1 is closed
    Need need = ownerProtocolOwnerClient.readNeed(needURI);
    Assert.assertEquals(needURI,need.getNeedURI());
    Assert.assertEquals(NeedState.INACTIVE, need.getState());

  }

  private URI createOwnerURI(){
    return URI.create("http://owner.com/op");
  }

  private URI createMatcherURI(){
    return URI.create("http://matcher.com/mp");
  }
}
