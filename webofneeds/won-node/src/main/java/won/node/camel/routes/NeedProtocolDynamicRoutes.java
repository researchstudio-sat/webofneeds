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

package won.node.camel.routes;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;

import java.util.List;

/**
 * User: LEIH-NB
 * Date: 25.11.13
 */
public class NeedProtocolDynamicRoutes extends RouteBuilder {

    List<String> endpoints;
    public NeedProtocolDynamicRoutes(CamelContext camelContext, List<String> Endpoints){
        super(camelContext);
        this.endpoints = endpoints;

    }
    @Override
    public void configure() throws Exception {
        from("seda:NeedProtocolOut")
                .to("log:Dynamic Route FROM NODE")
                .recipientList(header("remoteBrokerEndpoint"));


                /*.wireTap("bean:messagingService?method=inspectMessage")
                .choice()
                .when(property("remoteBrokerEndpoint").isNotNull())
                .to("seda:NeedProtocolOutRemote")
                .otherwise()
                .to("seda:NeedProtocolOutLocal");
        from("seda:NeedProtocolOutRemote")
                .to("$property.remoteBrokerEndpoint");
        from("seda:NeedProtocolOutLocal")
                .to(endpoints.get(0));     */
              /*
        for (int i = 0; i<endpoints.size();i++){
            from("seda:NeedProtocolOut")
                    .wireTap("bean:messagingService?method=inspectMessage")

            from(endpoints.get(i))
                    .wireTap("bean:messagingService?method=inspectMessage")
                    .choice()
                    .when(header("methodName").isEqualTo("connect"))
                    .to("log:OWNER CONNECT RECEIVED")
                    .to("bean:ownerProtocolOwnerService?method=connect")
                    .when(header("methodName").isEqualTo("hint"))
                    .to("bean:ownerProtocolOwnerService?method=hint")
                    .when(header("methodName").isEqualTo("textMessage"))
                    .to("bean:ownerProtocolOwnerService?method=textMessage")
                    .when(header("methodName").isEqualTo("open"))
                    .to("bean:ownerProtocolOwnerService?method=open")
                    .when(header("methodName").isEqualTo("close"))
                    .to("bean:ownerProtocolOwnerService?method=close")
                    .otherwise()
                    .to("log:Message Type Not Supported");
        }
        from("seda:NeedProtocolOut")


                .otherwise()
                    .when(header("methodName").isEqualTo("connect"))
                    .to("activemq:queue:WON.NeedProtocol.Connect.In")
                    .when(header("methodName").isEqualTo("open"))
                    .to("activemq:queue:WON.NeedProtocol.Open.In")
                    .when(header("methodName").isEqualTo("close"))
                    .to("activemq:queue:WON.NeedProtocol.Close.In")
                    .when(header("methodName").isEqualTo("textMessage"))
                    .to("activemq:queue:WON.NeedProtocol.TextMessage.In");     */

    }
}
