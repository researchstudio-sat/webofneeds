/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.matcher.camel.routes;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

//TODO: This route builder should not be loaded on startup, but it's being loaded..

/**
 * User: sbyim Date: 14.11.13 Each Won Node provides queues for owner
 * applications that are registered on the node. Owner Application shall
 * generate routes dynamically in runtime that listen to those queues.
 */
public class MatcherApplicationListenerRouteBuilder extends RouteBuilder {

  private List<String> endpoints;
  private URI brokerUri;

  public MatcherApplicationListenerRouteBuilder(CamelContext camelContext, Set<String> endpoints, URI remoteEndpoint) {
    super(camelContext);
    List<String> endpointsList = new ArrayList();
    Iterator iter = endpoints.iterator();
    while (iter.hasNext())
      for (int i = 0; i < endpoints.size(); i++) {
        String endpoint = "activemq" + remoteEndpoint.toString().replaceAll("[/:]", "") + ":topic:" + iter.next();
        endpointsList.add(endpoint);
      }
    this.endpoints = endpointsList;
    this.brokerUri = remoteEndpoint;
  }

  /**
   * @throws Exception
   */

  @Override
  public void configure() throws Exception {
    for (int i = 0; i < endpoints.size(); i++) {
      from(endpoints.get(i)).routeId("Node2MatcherRoute" + brokerUri + i).choice()
          .when(header("methodName").isEqualTo("needCreated"))
          .to("bean:matcherProtocolMatcherServiceJMSBased?method=needCreated")
          .when(header("methodName").isEqualTo("needActivated"))
          .to("bean:matcherProtocolMatcherServiceJMSBased?method=needActivated")
          .when(header("methodName").isEqualTo("needDeactivated"))
          .to("bean:matcherProtocolMatcherServiceJMSBased?method=needDeactivated").otherwise()
          .to("log:Message Type Not Supported");
    }
  }

}
