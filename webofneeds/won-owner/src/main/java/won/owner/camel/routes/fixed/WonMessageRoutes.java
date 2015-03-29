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

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.builder.RouteBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim
 * Date: 02.03.2015
 */
public class WonMessageRoutes extends RouteBuilder
{

  @Override
  public void configure() throws Exception {
    from("activemq:queue:OwnerProtocol.in?concurrentConsumers=5")
      .wireTap("bean:messagingService?method=inspectMessage")
      .routeId("WonMessageOwnerRoute")
      .setHeader(WonCamelConstants.DIRECTION_HEADER, new ConstantStringExpression(WONMSG.TYPE_FROM_OWNER_STRING))
        .choice()
          .when(header("methodName").isEqualTo("register"))
            .to("bean:ownerManagementService?method=registerOwnerApplication")
          .when(header("methodName").isEqualTo("getEndpoints"))
            .to("bean:queueManagementService?method=getEndpointsForOwnerApplication")
          .otherwise()
            .to("bean:wonMessageIntoCamelProcessor")
      .to("bean:wellformednessChecker")
            .to("bean:signatureChecker")
            .to("bean:ownerCallbackAdapter");
  }

  private class ConstantStringExpression implements Expression
  {
    private String constantString;

    public ConstantStringExpression(final String constantString) {
      this.constantString = constantString;
    }

    @Override
    public <T> T evaluate(final Exchange exchange, final Class<T> type) {
      return type.cast(constantString);
    }
  }
}
