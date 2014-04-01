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

package won.node.messaging;

import won.node.camel.routes.NeedProtocolDynamicRoutes;
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.jms.NeedBasedCamelConfiguratorImpl;

import java.net.URI;

//import won.node.camel.routes.NeedProtocolDynamicRoutes;

/**
 * User: LEIH-NB
 * Date: 26.02.14
 */
public class NeedProtocolCamelConfiguratorImpl extends NeedBasedCamelConfiguratorImpl {

    @Override
    public synchronized void addRouteForEndpoint(String startingComponent,URI brokerUri) throws CamelConfigurationFailedException {
      if (getCamelContext().getRoute(startingComponent)==null){
        NeedProtocolDynamicRoutes needProtocolRouteBuilder = new NeedProtocolDynamicRoutes(getCamelContext(),startingComponent);
        try {
          getCamelContext().addRoutes(needProtocolRouteBuilder);
        } catch (Exception e) {
          throw new CamelConfigurationFailedException("adding route to camel context failed",e);
        }
      }
    }

}
