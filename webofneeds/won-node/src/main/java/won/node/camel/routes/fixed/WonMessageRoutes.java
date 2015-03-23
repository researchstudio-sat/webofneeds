package won.node.camel.routes.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim
 * Date: 02.03.2015
 */
public class WonMessageRoutes  extends RouteBuilder
{

  @Override
  public void configure() throws Exception {
    from("activemq:queue:OwnerProtocol.in?concurrentConsumers=5")
      .routeId("WonMessageOwnerRoute")
      .setHeader("direction", new ConstantStringExpression(WONMSG.TYPE_FROM_OWNER_STRING))
        .choice()
          .when(header("methodName").isEqualTo("register"))
            .to("bean:ownerManagementService?method=registerOwnerApplication")
          .when(header("methodName").isEqualTo("getEndpoints"))
            .to("bean:queueManagementService?method=getEndpointsForOwnerApplication")
          .otherwise()
            .to("bean:wonMessageIntoCamelProcessor")
            .to("bean:wellformednessChecker")
            .to("bean:signatureChecker")
            .to("bean:wrapperFromOwner")
            .routingSlip(method("wonMessageSlipComputer"));
    from("activemq:queue:NeedProtocol.in?concurrentConsumers=5")
       .routeId("WonMessageNodeRoute")
       .setHeader("direction", new ConstantStringExpression(WONMSG.TYPE_FROM_EXTERNAL_STRING))
       .choice()
         .when(header("methodName").isEqualTo("register"))
          .to("bean:ownerManagementService?method=registerOwnerApplication")
         .when(header("methodName").isEqualTo("getEndpoints"))
          .to("bean:queueManagementService?method=getEndpointsForOwnerApplication")
         .otherwise()
            .to("bean:wonMessageIntoCamelProcessor")
            .to("bean:wellformednessChecker")
            .to("bean:signatureChecker")
            .to("bean:wrapperFromExternal")
            .routingSlip(method("wonMessageSlipComputer"));
    from("activemq:queue:MatcherProtocol.in?concurrentConsumers=5")
      .routeId("WonMessageMatcherRoute")
      .to("bean:wonMessageIntoCamelProcessor")
      .choice()
        .when(header("messageType").isEqualTo(WONMSG.TYPE_HINT))
          .to("bean:wellformednessChecker")
          .to("bean:signatureChecker")
          .to("bean:wrapperFromExternal")
          .to("bean:hintMessageProcessor?method=process")
          .to("bean:persister")
        .otherwise()
          .log(LoggingLevel.INFO, "could not route message");
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
