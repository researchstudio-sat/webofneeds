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
import won.server.protocol.MatcherProtocolExternalFacade;
import won.server.protocol.OwnerProtocolExternalFacade;

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
  private OwnerProtocolExternalFacade ownerProtocolExternalFacade;
  private MatcherProtocolExternalFacade matcherProtocolExternalFacade;
  private static final URI MATCHER_URI = URI.create("http://localhost/matcher");

  /**
   * Functional test that does not require a working owner protocol or matcher protocol.
   * Designed to be called inside the running web-application, only need protocol messages
   * are exchanged via HTTP.
   *
   * TODO: need representation has to be changed
   */
  @Test
  public void simpleTransactionTest1() {
    //simulate owner1: create need
    String need1Content = "Replace me with something useful!";
    URI need1URI = ownerProtocolExternalFacade.createNeed(need1Content);

    //simulate owner2: create need
    String need2Content = "Replace me with something useful, too!";
    URI need2URI = ownerProtocolExternalFacade.createNeed(need2Content);

    //simulate matcher: read needs, hint to both
    //NOTE: reading a need is assumed to be done from linked data publication - so we don't need a call here
    matcherProtocolExternalFacade.hint(need1URI, need2URI,0.95, MATCHER_URI);
    matcherProtocolExternalFacade.hint(need2URI, need1URI,0.95, MATCHER_URI);

    //simulate owner1: fetch matches
    Collection<Match> matches1 = ownerProtocolExternalFacade.getMatches(need1URI);
    Match match12 = matches1.iterator().next();
    assertEquals(need1URI, match12.fromNeed);
    assertEquals(need2URI, match12.toNeed);

    //simulate owner1: initiate connection
    ownerProtocolExternalFacade.connect(need1URI,need2URI,"I'm interested!");

    //simulate owner2: check status and find out about match and connection request
    Collection<Match> matches2 = ownerProtocolExternalFacade.getMatches(need1URI);
    Match match21 = matches2.iterator().next();
    assertEquals(need2URI, match21.fromNeed);
    assertEquals(need1URI, match21.toNeed);

    //simulate owner2: check status and find out about connection request
    Collection<URI> transactionList2 = ownerProtocolExternalFacade.listTransactionURIs(need2URI);
    //simulate owner2: read transaction description
    URI transaction2URI = transactionList2.iterator().next();
    String transaction2 = ownerProtocolExternalFacade.readTransaction(transaction2URI);
    //TODO: from transaction representation the connection request can be read. Do that to get need1URI
    //simulate owner2: accept connection
    ownerProtocolExternalFacade.acceptConnect(need1URI, transaction2URI);
    //simulate owner1: send message
    //TODO: continue here!
    //simulate owner2: send message
    //simulate owner1: finish transaction
    //simulate owner2: finish transaction
  }

}
