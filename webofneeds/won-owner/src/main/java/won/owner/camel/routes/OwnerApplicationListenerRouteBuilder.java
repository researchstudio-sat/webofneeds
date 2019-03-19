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

package won.owner.camel.routes;

import java.net.URI;

import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.vocabulary.WONMSG;

//TODO: This route builder should not be loaded on startup, but it's being loaded..
/**
 * User: sbyim Date: 14.11.13 Each Won Node provides queues for owner applications that are registered on the node.
 * Owner Application shall generate routes dynamically in runtime that listen to those queues.
 */
public class OwnerApplicationListenerRouteBuilder extends RouteBuilder {

    private String endpoint;
    private URI brokerUri;

    public OwnerApplicationListenerRouteBuilder(CamelContext camelContext, String endpoint, URI remoteEndpoint) {
        super(camelContext);
        this.endpoint = endpoint;
        this.brokerUri = remoteEndpoint;
    }

    /**
     *
     * @throws Exception
     */

    @Override
    public void configure() throws Exception {

        // we remove the concurrentConsumers part from
        // the URI as it makes it hard to check if a given endpoint is already configured in the context by searching
        // for its
        // name. Also, we're unsure if the concurrentConsumers part is even interpreted anywhere //
        // from(endpoints.get(i)
        // +"?concurrentConsumers=2")
        from(endpoint).routeId("Node2OwnerRoute" + brokerUri).to("bean:wonMessageIntoCamelProcessor")
                .to("bean:wellformednessChecker").to("bean:uriNodePathChecker").choice()
                .when(header(WonCamelConstants.MESSAGE_TYPE_HEADER).isEqualTo(URI.create(WONMSG.TYPE_HINT_STRING)))
                // don't check the signature if we're processing a hint message (until the matcher signs its messages)
                .log(LoggingLevel.DEBUG, "not checking signature because we're  processing a hint message)")
                .to("bean:linkedDataCacheInvalidator").to("bean:linkedDataCacheUpdater")
                // this expects a bean with name 'mainOwnerMessageProcessor' in the application context
                // this bean is *not* provided by the won-owner module. This allows the definition of a
                // different processing chain depending on the use case.
                .to("bean:mainOwnerMessageProcessor").otherwise()
                .log(LoggingLevel.DEBUG, "checking signature because we're not processing a hint message)")
                .to("bean:signatureChecker").to("bean:linkedDataCacheInvalidator").to("bean:linkedDataCacheUpdater")
                // this expects a bean with name 'mainOwnerMessageProcessor' in the application context
                // this bean is *not* provided by the won-owner module. This allows the definition of a
                // different processing chain depending on the use case.
                .to("bean:mainOwnerMessageProcessor").end();

    }

}
