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
    /**
     * owner protocol, incoming
     */
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
    /**
     * Owner protocol, outgoing
     */
    from("seda:OwnerProtocolOut?concurrentConsumers=5").routeId("Node2OwnerRoute")
            .to("bean:ownerProtocolOutgoingMessagesProcessor")
            .recipientList(header("ownerApplicationIDs"));
    /**
     * Need protocol, incoming
     */
    from("activemq:queue:NeedProtocol.in?concurrentConsumers=5")
      .to("seda:NeedProtocolIn");
    /**
     * Need protocol, incoming from local
     */
    from("seda:NeedProtocolIn")
       .routeId("WonMessageNodeRoute")
       .setHeader("direction", new ConstantStringExpression(WONMSG.TYPE_FROM_EXTERNAL_STRING))
       .choice()
            // (won nodes register the same way as owner applications do)
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
    /**
     * Need protocol, outgoing
     */
    from("seda:NeedProtocolOut?concurrentConsumers=5").routeId("Node2NodeRoute")
            .to("bean:needProtocolOutgoingMessagesProcessor");
    /**
     * Matcher protocol, incoming
     */
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
    /**
     * Outgoing messages - routing by 'protocol' header
     */
    from("seda:OUTMSG").routeId("ProtocolSelectorRoute")
            .wireTap("bean:messagingService?method=inspectMessage")
            .choice()
            .when(header("protocol").isEqualTo("OwnerProtocol"))
            .to("seda:OwnerProtocolOut")
            .when(header("protocol").isEqualTo("MatcherProtocol"))
            .to("seda:MatcherProtocolOut")
            .otherwise()
            .to("log:No protocol defined in header");

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
