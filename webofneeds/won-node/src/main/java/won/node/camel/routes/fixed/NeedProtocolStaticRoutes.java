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
 * Date: 25.11.13
 */
public class NeedProtocolStaticRoutes extends RouteBuilder {
    @Override
    public void configure() throws Exception {
       /* from("seda:NeedProtocolOut")
                .wireTap("bean:messagingService?method=inspectMessage")
                .choice()
                .when(property("remoteBrokerEndpoint").isNull())
                .when(header("methodName").isEqualTo("connect"))
                .to("activemq:queue:WON.NeedProtocol.Connect.In")
                .when(header("methodName").isEqualTo("open"))
                .to("activemq:queue:WON.NeedProtocol.Open.In")
                .when(header("methodName").isEqualTo("close"))
                .to("activemq:queue:WON.NeedProtocol.Close.In")
                .when(header("methodName").isEqualTo("textMessage"))
                .to("activemq:queue:WON.NeedProtocol.TextMessage.In");   */
        from("activemq:queue:WON.NeedProtocol.Connect.In")
                .to("log:Routing message from queue WON.NeedProtocol.Connect.In")
                .to("bean:needProtocolNeedServiceJMSBased?method=connect");
        from("activemq:queue:WON.NeedProtocol.Open.In")
                .to("log:Routing message from queue WON.NeedProtocol.Open.In")
                .to("bean:needProtocolNeedServiceJMSBased?method=open");
        from("activemq:queue:WON.NeedProtocol.Close.In")
                .to("log:Routing message from queue WON.NeedProtocol.Close.In")
                .to("bean:needProtocolNeedServiceJMSBased?method=close");
        from("activemq:queue:WON.NeedProtocol.TextMessage.In")
                .to("log:Routing message from queue WON.NeedProtocol.TextMessage.In")
                .to("bean:needProtocolNeedServiceJMSBased?method=textMessage");
        from("activemq:queue:NeedProtocol.in")
                .choice()
                .when(header("methodName").isEqualTo("connect"))
                .to("log:Connect Incoming")
                .to("bean:needProtocolNeedServiceJMSBased?method=connect")
                .when(header("methodName").isEqualTo("open"))
                .to("bean:needProtocolNeedServiceJMSBased?method=open")
                .when(header("methodName").isEqualTo("close"))
                .to("bean:needProtocolNeedServiceJMSBased?method=close")
                .when(header("methodName").isEqualTo("textMessage"))
                .to("bean:needProtocolNeedServiceJMSBased?method=textMessage")
                .otherwise()
                .to("log:Not supported");

    }
}
