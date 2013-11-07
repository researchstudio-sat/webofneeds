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


package won.node.routes;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;

/**
 * User: LEIH-NB
 * Date: 10.10.13
 */
public class AmqpToJms extends RouteBuilder{
    @Override
    public void configure(){
        from("activemq:queue:WON.CREATENEED")
                .to("log:INMSG")
                .to("bean:ownerProtocolNeedJMSService?method=createNeed");
               // beanRef("ownerProtocolNeedJMSService","createNeed") ;
        from("activemq:queue:WON.CONNECTNEED")
                .to("log:CONNECTNEED IN")
                .to("bean:ownerProtocolNeedJMSService?method=connect");
        from("activemq:queue:WON.ACTIVATENEED")
                .to("log:ACTIVATENEED IN")
                .to("bean:ownerProtocolNeedJMSService?method=activate");
        from("activemq:queue:WON.DEACTIVATENEED")
                .to("log:DEACTIVATENEED IN")
                .to("bean:ownerProtocolNeedJMSService?method=deactivate");
        from("activemq:queue:WON.OPEN")
                .to("log:OPEN IN")
                .to("bean:ownerProtocolNeedJMSService?method=open");
        from("activemq:queue:WON.CLOSE")
                .to("log:CLOSE IN")
                .to("bean:ownerProtocolNeedJMSService?method=close");
        from("activemq:queue:WON.TEXTMESSAGE")
                .to("log:TEXTMESSAGE IN")
                .to("bean:ownerProtocolNeedJMSService?method=textMessage");
        from("activemq:queue:WON.INMSG")
            .choice()
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
        from("seda:OUTMSG")
                .to("log:OUTMSG FROM NODE")
                .choice()
                    .when(property("methodName").isEqualTo("connect"))
                    .to("activemq:queue:WON.NeedProtocol.Connect.In")
                    .when(property("methodName").isEqualTo("open"))
                    .to("activemq:queue:WON.NeedProtocol.Open.In")
                    .when(property("methodName").isEqualTo("close"))
                    .to("activemq:queue:WON.NeedProtocol.Close.In")
                    .when(property("methodName").isEqualTo("textMessage"))
                    .to("activemq:queue:WON.NeedProtocol.TextMessage.In");
        from("activemq:queue:WON.NeedProtocol.Connect.In")
                .to("log:Routing message from queue WON.NeedProtocol.Connect.In")
                .to("bean:needProtocolNeedService?method=connect");
        from("activemq:queue:WON.NeedProtocol.Open.In")
                .to("log:Routing message from queue WON.NeedProtocol.Open.In")
                .to("bean:needProtocolNeedService?method=open");
        from("activemq:queue:WON.NeedProtocol.Close.In")
                .to("log:Routing message from queue WON.NeedProtocol.Close.In")
                .to("bean:needProtocolNeedService?method=close");
        from("activemq:queue:WON.NeedProtocol.TextMessage.In")
                .to("log:Routing message from queue WON.NeedProtocol.TextMessage.In")
                .to("bean:needProtocolNeedService?method=textMessage");
    }
}
