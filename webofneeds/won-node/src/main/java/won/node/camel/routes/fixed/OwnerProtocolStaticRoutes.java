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
public class OwnerProtocolStaticRoutes extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("activemq:queue:OwnerProtocol.in?concurrentConsumers=5")
            .wireTap("bean:messagingService?method=inspectMessage")
            .choice()
            .when(header("methodName").isEqualTo("register"))
            .to("bean:ownerProtocolNeedJMSService?method=registerOwnerApplication")
            .when(header("methodName").isEqualTo("getEndpoints"))
            .to("bean:ownerProtocolNeedJMSService?method=getEndpointsForOwnerApplication")
            .when(header("methodName").isEqualTo("createNeed"))
            .to("bean:ownerProtocolNeedJMSService?method=createNeed")
            .when(header("methodName").isEqualTo("connect"))
            .to("bean:ownerProtocolNeedJMSService?method=connect")
            .when(header("methodName").isEqualTo("activate"))
            .to("bean:ownerProtocolNeedJMSService?method=activate")
            .when(header("methodName").isEqualTo("deactivate"))
            .to("bean:ownerProtocolNeedJMSService?method=deactivate")
            .when(header("methodName").isEqualTo("open"))
            .to("bean:ownerProtocolNeedJMSService?method=open")
            .when(header("methodName").isEqualTo("close"))
            .to("bean:ownerProtocolNeedJMSService?method=close")
            .when(header("methodName").isEqualTo("textMessage"))
            .to("log:Route. Owner Protocol TextMessage Received")
            .to("bean:ownerProtocolNeedJMSService?method=textMessage")
            .when(header("methodName").isEqualTo("registerOwnerApplication"))
            .to("log:Route. Owner Protocol Register Received")
            .to("bean:ownerProtocolNeedJMSService?method=registerOwnerApplication")
            .when(header("methodName").isEqualTo("getEndpointsForOwnerApplication"))
            .to("log:Route. Owner Protocol getEndpoints Received")
            .to("bean:ownerProtocolNeedJMSService?method=getEndpointsForOwnerApplication")
            .otherwise()
            .to("bean:ownerProtocolNeedJMSService?method=close");
    }
}
