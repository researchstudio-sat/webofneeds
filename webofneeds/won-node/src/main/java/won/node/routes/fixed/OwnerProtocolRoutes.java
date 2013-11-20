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

package won.node.routes.fixed;

import org.apache.camel.builder.RouteBuilder;

/**
 * User: LEIH-NB
 * Date: 19.11.13
 */
public class OwnerProtocolRoutes extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("seda:OUTMSG")
                .to("log:OUTMSG FROM NODE")
                .choice()
                .when(header("protocol").isEqualTo("NeedProtocol"))
                .to("seda:NeedProtocolOut")
                .when(header("protocol").isEqualTo("OwnerProtocol"))
                .to("seda:OwnerProtocolOut")
                .otherwise()
                .to("log:No protocol defined in header");
        from("seda:OwnerProtocolOut")
                .to("bean:ownerProtocolOutgoingMessagesProcessor")
                .wireTap("bean:messagingService?method=inspectMessage")
                .recipientList(header("ownerApplicationIDs"));
    }
}
