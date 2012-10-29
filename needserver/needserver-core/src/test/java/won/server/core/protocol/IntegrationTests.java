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

package won.server.core.protocol;

import org.junit.Test;
import won.server.need.Match;
import won.server.protocol.ConnectionService;
import won.server.protocol.NeedContainerService;
import won.server.protocol.NeedService;
import won.server.protocol.OwnerService;

import java.net.URI;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 *
 * User: fkleedorfer
 * Date: 15.10.12
 */

public class IntegrationTests
{
  private NeedContainerService needContainerService;
  private NeedService needService;
  private OwnerService ownerService;
  private ConnectionService connectionService;

  private static final URI MATCHER_URI = URI.create("http://localhost/matcher");

  /**
   * Functional test that does not require a working owner protocol or matcher protocol.
   * Designed to be called inside the running web-application, only needContainerService protocol messages
   * are exchanged via HTTP.
   *
   * TODO: needContainerService representation has to be changed
   */
  @Test
  public void simpleTransactionTest1() {
    //simulate owner1: create needContainerService
    String need1Content = "Replace me with something useful!";
    URI need1URI = needContainerService.createNeed(need1Content);

    //simulate owner2: create needContainerService
    String need2Content = "Replace me with something useful, too!";
    URI need2URI = needContainerService.createNeed(need2Content);

    //TODO: inside the server, how do we instantiate need objects?


    //simulate matcher: read needs, hint to both
    //NOTE: reading a needContainerService is assumed to be done through published linked data - so we don't needContainerService a call here
    needService.hint(need1URI, need2URI, 0.95, MATCHER_URI);
    needService.hint(need2URI, need1URI,0.95, MATCHER_URI);

    //simulate owner1: fetch matches
    Collection<Match> matches1 = needService.getMatches(need1URI);
    Match match12 = matches1.iterator().next();
    assertEquals(need1URI, match12.fromNeed);
    assertEquals(need2URI, match12.toNeed);

    //simulate owner1: initiate connection
    needService.connectTo(need1URI, need2URI, "I'm interested!");

    //simulate owner2: check status and find out about match and connection request
    Collection<Match> matches2 = needService.getMatches(need1URI);
    Match match21 = matches2.iterator().next();
    assertEquals(need2URI, match21.fromNeed);
    assertEquals(need1URI, match21.toNeed);

    //simulate owner2: check status and find out about connection request
    Collection<URI> transactionList2 = needService.listConnectionURIs(need2URI);
    //simulate owner2: read transaction description
    URI transaction2URI = transactionList2.iterator().next();
    String transaction2 = connectionService.read(transaction2URI);
    //TODO: from transaction representation the connection request can be read. Do that to get need1URI
    //simulate owner2: accept connection
    connectionService.accept(transaction2URI);
    //simulate owner1: send message
    //TODO: continue here!
    //simulate owner2: send message

    //simulate owner1: finish transaction
    //simulate owner2: finish transaction
  }

}
