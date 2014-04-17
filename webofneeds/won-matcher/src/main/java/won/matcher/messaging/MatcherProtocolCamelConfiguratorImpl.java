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

package won.matcher.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.camel.routes.MatcherApplicationListenerRouteBuilder;
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.jms.MatcherProtocolCamelConfigurator;
import won.protocol.jms.NeedBasedCamelConfiguratorImpl;

import java.net.URI;
import java.util.Set;

//import won.node.camel.routes.NeedProtocolDynamicRoutes;

/**
 * User: LEIH-NB
 * Date: 26.02.14
 */
public class MatcherProtocolCamelConfiguratorImpl extends NeedBasedCamelConfiguratorImpl implements
  MatcherProtocolCamelConfigurator{

  private Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public synchronized void addRemoteTopicListeners(final Set<String> endpoints, final URI remoteEndpoint)
    throws CamelConfigurationFailedException {
    logger.info("length of endpoints {}", endpoints.size());
    MatcherApplicationListenerRouteBuilder matcherApplicationListenerRouteBuilder = new
    MatcherApplicationListenerRouteBuilder(getCamelContext(),endpoints, remoteEndpoint);

    try {
      getCamelContext().addRoutes(matcherApplicationListenerRouteBuilder);
    } catch (Exception e) {
      logger.debug("adding route to camel context failed", e);
      throw new CamelConfigurationFailedException("adding route to camel context failed",e);
    }
  }
}
