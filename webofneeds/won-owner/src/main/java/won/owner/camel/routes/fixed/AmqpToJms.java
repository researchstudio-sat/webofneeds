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


package won.owner.camel.routes.fixed;
import org.apache.camel.builder.RouteBuilder;
/**
 * User: LEIH-NB
 * Date: 10.10.13
 */
//TODO: change to asyncronous processing maybe
public class AmqpToJms extends RouteBuilder{
    @Override
    public void configure(){
        from("seda:outgoingMessages")
                //todo: broker endpoint negotiation shall be run here and not in the service client classes.
                .wireTap("bean:messagingService?method=inspectMessage")
                .recipientList(header("remoteBrokerEndpoint"));

      /*  from("seda:OUTMSG")
                .to("log:OUTMSG")
                .choice()
                    .when(header("methodName").isEqualTo("register"))
                    .to("activemq:queue:WON.REGISTER")
                    .when(header("methodName").isEqualTo("getEndpoints"))
                    .to("activemq:queue:WON.GETENDPOINTS");     */

    }

}
