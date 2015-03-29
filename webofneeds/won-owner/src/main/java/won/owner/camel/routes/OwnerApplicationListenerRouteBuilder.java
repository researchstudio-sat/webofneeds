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

package won.owner.camel.routes;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;

import java.net.URI;
import java.util.List;

//TODO: This route builder should not be loaded on startup, but it's being loaded..
/**
 * User: sbyim
 * Date: 14.11.13
 * Each Won Node provides queues for owner applications that are registered on the node.
 * Owner Application shall generate routes dynamically in runtime that listen to those queues.
 */
public class OwnerApplicationListenerRouteBuilder extends RouteBuilder  {


    private List<String> endpoints;
    private URI brokerUri;

    public OwnerApplicationListenerRouteBuilder(CamelContext camelContext, List<String> endpoints, URI remoteEndpoint) {
        super(camelContext);
        this.endpoints = endpoints;
        this.brokerUri = remoteEndpoint;
    }

    /**
     *
     * @throws Exception
     */

    @Override
    public void configure() throws Exception {
               for (int i = 0; i<endpoints.size();i++){
                   from(endpoints.get(i)+"?concurrentConsumers=5").routeId("Node2OwnerRoute"+brokerUri)
                           .wireTap("bean:messagingService?method=inspectMessage")
                            .to("bean:wonMessageIntoCamelProcessor")
                            .to("bean:wellformednessChecker")
                            .to("bean:signatureChecker")
                            .to("bean:ownerCallbackAdapter");
       }
    }


}
