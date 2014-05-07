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

package won.node.camel.routes.fixed;

import org.apache.camel.builder.RouteBuilder;

/**
 * User: LEIH-NB
 * Date: 27.11.13
 */
public class MatcherProtocolStaticRoutes extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("activemq:queue:MatcherProtocol.in?concurrentConsumers=5").routeId("Matcher2NodeRoute")
            .wireTap("bean:messagingService?method=inspectMessage")
            .choice()
            .when(header("methodName").isEqualTo("hint"))
            .to("bean:matcherProtocolNeedJMSService?method=hint");
        from("seda:MatcherProtocolOut?concurrentConsumers=5").routeId("Node2MatcherRoute")
            .choice()
            .when(header("methodName").isEqualTo("matcherRegistered"))
            .to("activemq:topic:MatcherProtocol.Out.Matcher")
            .otherwise()
            .to("activemq:topic:MatcherProtocol.Out.Need");
    }
}
