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

package won.owner.routes;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;

import java.util.List;

/**
 * User: sbyim
 * Date: 25.11.13
 * This class is used to dynamically generate routes for outgoing messages from owner application.
 */
public class OwnerProtocolDynamicRoutes extends RouteBuilder {


    List<String> endpoints;
    String routeID;
    String from;

    /**
     *
     * @param camelContext the camelContext where the routes are added
     * @param Endpoints
     * @param from each route has the starting consuming endpoint per node it wants to send the messages to.
     */
    public OwnerProtocolDynamicRoutes(CamelContext camelContext, List<String> Endpoints, String from){
        super(camelContext);
        //TODO: consider if we need these variables.
        this.endpoints = endpoints;
        this.routeID = routeID;
        this.from = from;

    }

    /**
     * generates the route. the recipient is defined in the message header.
     * @throws Exception
     */
    @Override
    public void configure() throws Exception {
        from(from).routeId(from)
                //.wireTap("bean:messagingService?method=inspectMessage")
                .to("log:Dynamic Route FROM Owner")
                .recipientList(header("remoteBrokerEndpoint"));

    }
}
