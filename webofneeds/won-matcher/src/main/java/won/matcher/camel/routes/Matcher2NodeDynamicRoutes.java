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

import java.util.List;

/**
 * User: LEIH-NB Date: 10.10.13
 */

public class Matcher2NodeDynamicRoutes extends RouteBuilder {
  private List<String> endpoints;
  private String routeID;
  private String from;

  public Matcher2NodeDynamicRoutes(CamelContext camelContext, String from) {
    super(camelContext);
    this.from = from;

  }

  @Override
  public void configure() {
    from("seda:MatcherProtocol.Out.Hint?concurrentConsumers=2").routeId("Matcher2NodeRoute")
        .recipientList(header("remoteBrokerEndpoint"));

  }

}
