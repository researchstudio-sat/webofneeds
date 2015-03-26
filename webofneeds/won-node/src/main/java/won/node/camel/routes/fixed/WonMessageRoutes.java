package won.node.camel.routes.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import won.node.messaging.predicates.IsMessageForConnectionPredicate;
import won.node.messaging.predicates.IsResponseMessagePredicate;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim
 * Date: 02.03.2015
 */
public class WonMessageRoutes  extends RouteBuilder
{

  @Override
  public void configure() throws Exception {

    onException(WonMessageProcessingException.class)
      .handled(true)
      .to("bean:failResponder")
      .wireTap("bean:messagingService?method=inspectMessage");



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
            //route to msg processing logic
            .to("seda:OwnerProtocolLogic");
    /**
     * Owner protocol, outgoing
     */
    from("seda:OwnerProtocolOut?concurrentConsumers=5").routeId("Node2OwnerRoute")
            .to("bean:ownerProtocolOutgoingMessagesProcessor")
            .recipientList(header("ownerApplicationIDs"));
    /**
     * System messages: treated almost as incoming from owner.
     */
    from("seda:SystemProtocolIntoOwnerProtocol?concurrentConsumers=5")
      .routeId("WonMessageSystemRoute")
      .setHeader("direction", new ConstantStringExpression(WONMSG.TYPE_FROM_SYSTEM_STRING))
      .to("bean:wonMessageIntoCamelProcessor")
      .to("bean:wellformednessChecker")
      .to("bean:signatureChecker")
      .to("bean:wrapperFromSystem")
      //route to message processing logic
      .to("seda:OwnerProtocolLogic");

    /**
     * Owner protocol logic: expects messages from OwnerProtocolIn and SystemIntoOwnerProtocol routes.
     */
    from("seda:OwnerProtocolLogic?concurrentConsumers=5")
        //call the default implementation, which may alter the message.
        // Also, it puts any outbound message in the respective header
      .routingSlip(method("messageTypeSlip"))
      .to("bean:persister")
      .setHeader(WonCamelConstants.WON_MESSAGE_HEADER, header(WonCamelConstants.OUTBOUND_MESSAGE_HEADER))
        //now if the outbound message is one that facet implementations can
        //process, let them do that, then send the resulting message to the remote end.
      .filter(new IsMessageForConnectionPredicate())
      .routingSlip(method("facetTypeSlip"))
      .to("bean:toNodeSender")
        //if we didn't raise an exception so far, send a success response
      .to("bean:successResponder");

    /**
     * Need protocol, incoming
     */
    from("activemq:queue:NeedProtocol.in?concurrentConsumers=5")
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
            //call the default implementation, which may alter the message.
            .routingSlip(method("messageTypeSlip"))
            .to("bean:persister")
            //inbound messages are always passed to facet implementations
            .routingSlip(method("facetTypeSlip"))
            //now, we have the message we want to pass on to the owner in the exchange's in header.
            //do we want to send a response back? only if we're not currently processing a response!
            .to("bean:toOwnerSender")
            .filter(PredicateBuilder.not(new IsResponseMessagePredicate()))
                .to("bean:successResponder");

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
        //we only handle hint messages
        .when(header("messageType").isEqualTo(WONMSG.TYPE_HINT))
          .to("bean:wellformednessChecker")
          .to("bean:signatureChecker")
          .to("bean:wrapperFromExternal")
          .to("bean:hintMessageProcessor?method=process")
          .to("bean:persister")
          .to("bean:toOwnerSender")
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
