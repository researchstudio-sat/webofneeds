/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.node.camel;

import java.net.URI;

import org.apache.camel.RoutesBuilder;

import won.node.camel.route.AtomProtocolDynamicRoutes;
import won.protocol.jms.AtomBasedCamelConfiguratorImpl;

// import won.node.camel.routes.AtomProtocolDynamicRoutes;
/**
 * User: LEIH-NB Date: 26.02.14
 */
public class AtomProtocolCamelConfiguratorImpl extends AtomBasedCamelConfiguratorImpl {
    @Override
    protected RoutesBuilder createRoutesBuilder(final String startingEndpoint, final URI brokerUri) {
        return new AtomProtocolDynamicRoutes(getCamelContext(), startingEndpoint);
    }
}
