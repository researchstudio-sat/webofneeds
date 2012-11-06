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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import won.owner.service.impl.AutomaticOwnerService;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.matcher.MatcherProtocolNeedService;
import won.protocol.model.NeedState;
import won.protocol.need.NeedProtocolNeedService;
import won.protocol.owner.OwnerProtocolNeedService;
import won.protocol.service.OwnerFacingNeedCommunicationService;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 06.11.12
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/applicationContext.xml","/services.xml", "/mockClients.xml"})
@TransactionConfiguration(transactionManager="transactionManager", defaultRollback=false)
@Transactional
public class NeedServerIntegrationTests
{
  @Autowired
  private NeedProtocolNeedService needProtocolNeedService;
  @Autowired
  private OwnerProtocolNeedService ownerProtocolNeedService;
  @Autowired
  private MatcherProtocolNeedService matcherProtocolNeedService;
  @Autowired
  private OwnerFacingNeedCommunicationService needCommunicationService;
  @Autowired
  private AutomaticOwnerService mockOwnerService;


  @Test
  public void testNeedCreation(){
    URI ownerURI = createOwnerURI();
    URI matcherURI = createMatcherURI();
    URI need1URI = ownerProtocolNeedService.createNeed(ownerURI,null,false);
    URI need2URI = ownerProtocolNeedService.createNeed(ownerURI,null,false);
    Assert.assertNotSame("Two consecutively created needs have the same URI", need1URI, need2URI);

    //try to connect the needs (both are inactive)
    try {
      URI connectionURI = ownerProtocolNeedService.connectTo(need1URI,need2URI,"If this connection is created, there's something wrong!");
      Assert.fail("Expected exception not thrown");
    } catch (IllegalMessageForNeedStateException e){
      Assert.assertEquals(need1URI,e.getNeedURI());
      Assert.assertEquals("CONNECT_TO",e.getMethodName());
      Assert.assertEquals(NeedState.INACTIVE,e.getNeedState());
    }

    //try that from the other need, too
    try {
      URI connectionURI = ownerProtocolNeedService.connectTo(need2URI,need1URI,"If this connection is created, there's something wrong!");
      Assert.fail("Expected exception not thrown");
    } catch (IllegalMessageForNeedStateException e){
      Assert.assertEquals(need2URI,e.getNeedURI());
      Assert.assertEquals("CONNECT_TO",e.getMethodName());
      Assert.assertEquals(NeedState.INACTIVE,e.getNeedState());
    }

    //try to give a hint
    try {
      matcherProtocolNeedService.hint(need1URI,need2URI,1.0,matcherURI);
      Assert.fail("Expected exception not thrown");
    } catch (IllegalMessageForNeedStateException e){
      Assert.assertEquals(need1URI,e.getNeedURI());
      Assert.assertEquals("HINT",e.getMethodName());
      Assert.assertEquals(NeedState.INACTIVE,e.getNeedState());
    }
  }

  @Test
  public void testConnect() throws Exception{
    URI ownerURI = createOwnerURI();
    URI matcherURI = createMatcherURI();

    //prepare a mockup owner service that connects, sends a message and closes


    mockOwnerService.setAutoConnect(true);
    mockOwnerService.setOnAcceptAction("MESSAGE");
    mockOwnerService.setOnMessageAction("CLOSE");
    mockOwnerService.setOnCloseAction("NONE");
    mockOwnerService.setOwnerProtocolNeedService(ownerProtocolNeedService);

    URI need1URI = ownerProtocolNeedService.createNeed(ownerURI,null,true);
    URI need2URI = ownerProtocolNeedService.createNeed(ownerURI,null,true);
    Assert.assertNotSame("Two consecutively created needs have the same URI", need1URI, need2URI);

    //try to connect the needs (both are inactive)
    URI connectionURI = ownerProtocolNeedService.connectTo(need1URI,need2URI,"Let's talk!");
    Assert.assertNotNull(connectionURI);
    Thread.sleep(500);
    Assert.assertEquals(1,mockOwnerService.getMethodCallCount(AutomaticOwnerService.Method.accept));
    Assert.assertEquals(1,mockOwnerService.getMethodCallCount(AutomaticOwnerService.Method.sendTextMessage));
    Assert.assertEquals(1,mockOwnerService.getMethodCallCount(AutomaticOwnerService.Method.close));

  }

  private URI createOwnerURI(){
    return URI.create("http://owner.com/op");
  }

  private URI createMatcherURI(){
    return URI.create("http://matcher.com/mp");
  }
}
