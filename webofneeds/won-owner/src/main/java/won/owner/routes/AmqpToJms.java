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
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
/**
 * User: LEIH-NB
 * Date: 10.10.13
 */
//TODO: change to asyncronous processing maybe
public class AmqpToJms extends RouteBuilder{
    @Override
    public void configure(){
        from("direct:OUTMSG")
                .to("log:OUTMSG")
                .to(ExchangePattern.InOut, "activemq:queue:WON.CREATENEED") ;
        from("seda:OUTMSG")
                .to("log:OUTMSG")
                .choice()
                    .when(property("methodName").isEqualTo("register"))
                    .to("activemq:queue:WON.REGISTER")
                    .when(property("methodName").isEqualTo("connect"))
                    .to("activemq:queue:WON.CONNECTNEED")
                    .when(property("methodName").isEqualTo("activate"))
                    .to("log:ACTIVATEMSG")
                    .to("activemq:queue:WON.ACTIVATENEED")
                    .when(property("methodName").isEqualTo("deactivate"))
                    .to("activemq:queue:WON.DEACTIVATENEED")
                    .when(property("methodName").isEqualTo("textMessage"))
                    .to("activemq:queue:WON.TEXTMESSAGE")
                    .when(property("methodName").isEqualTo("createNeed"))
                    .to("activemq:queue:WON.CREATENEED")
                    .when(property("methodName").isEqualTo("open"))
                    .to("activemq:queue:WON.OPEN")
                    .when(property("methodName").isEqualTo("close"))
                    .to("activemq:queue:WON.CLOSE")
                    .otherwise()
                    .to("log:UNSUPPORTED METHOD") ;

        from("seda:OUTMSG1")
                   .to("activemq:queue:WON.INMSG");

        // beanRef("ownerProtocolNeedJMSService","createNeed") ;
    }

}
