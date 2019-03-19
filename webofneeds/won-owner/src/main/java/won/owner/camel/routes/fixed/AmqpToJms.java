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

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

/**
 * User: LEIH-NB Date: 10.10.13
 */

// TODO: change to asyncronous processing maybe
public class AmqpToJms extends RouteBuilder {
    @Override
    public void configure() {
        from("seda:outgoingMessages?concurrentConsumers=5").routeId("Owner2NodeRoute").choice()
                .when(header("remoteBrokerEndpoint").isNull())
                .log(LoggingLevel.ERROR, "could not route message: remoteBrokerEndpoint is null")
                .throwException(new IllegalArgumentException("could not route message: remoteBrokerEndpoint is null"))
                .otherwise().recipientList(header("remoteBrokerEndpoint"));
    }

}
