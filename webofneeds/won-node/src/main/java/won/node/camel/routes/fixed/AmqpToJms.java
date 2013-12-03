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
 * Date: 10.10.13
 */
public class AmqpToJms extends RouteBuilder{



    @Override
    public void configure(){
        from("seda:OUTMSG")
                //.wireTap("bean:messagingService?method=inspectMessage")
                .to("log:OUTMSG FROM NODE")
                .choice()
                .when(header("protocol").isEqualTo("NeedProtocol"))
                .to("log:NeedProtocol FROM NODE")
                .to("seda:NeedProtocolOut")
                .when(header("protocol").isEqualTo("OwnerProtocol"))
                .to("log:OwnerProtocol FROM NODE")
                .to("seda:OwnerProtocolOut")
                .otherwise()
                .to("log:No protocol defined in header");
        from("activemq:queue:WON.REGISTER")
                .to("bean:ownerProtocolNeedJMSService?method=registerOwnerApplication");
        from("activemq:queue:WON.GETENDPOINTS")
                .to("log:Get Endpoints IN")
                .to("bean:ownerProtocolNeedJMSService?method=getEndpointsForOwnerApplication");


    }


}
