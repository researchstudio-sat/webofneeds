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
    }
}
