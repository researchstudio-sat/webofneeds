package won.node.camel.route.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import won.node.camel.predicate.ShouldCallFacetImplForMessagePredicate;
import won.node.camel.predicate.IsResponseMessagePredicate;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: syim
 * Date: 02.03.2015
 */
public class WonMessageRoutes  extends RouteBuilder
{

  @Override
  public void configure() throws Exception {

    onException(Exception.class)
      .to("bean:failResponder")
      .handled(true)
      .wireTap("bean:messagingService?method=inspectMessage");



    /**
     * owner protocol, incoming
     */
    from("activemq:queue:OwnerProtocol.in?concurrentConsumers=5")
      .routeId("WonMessageOwnerRoute")
      .setHeader(WonCamelConstants.DIRECTION_HEADER, new ConstantURIExpression(URI.create(WONMSG.TYPE_FROM_OWNER_STRING)))
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
    from("seda:OwnerProtocolOut?concurrentConsumers=5")
      .routeId("Node2OwnerRoute")
      .to("bean:ownerProtocolOutgoingMessagesProcessor")
      .recipientList(header("ownerApplicationIDs"));

    /**
     * System-to-remote messages: treated almost as incoming from owner.
     */
    from("seda:SystemMessageToRemoteNode?concurrentConsumers=5")
      .routeId("SystemMessageToRemoteNode")
      .setHeader(WonCamelConstants.DIRECTION_HEADER, new ConstantURIExpression(URI.create(WONMSG.TYPE_FROM_SYSTEM_STRING)))
      .to("bean:wonMessageIntoCamelProcessor")
      .to("bean:wellformednessChecker")
      .to("bean:signatureChecker")
      .to("bean:wrapperFromSystem")
      //route to message processing logic
      .to("seda:OwnerProtocolLogic");

    /**
     * System-to-owner messages: treated almost as incoming from remote node.
     */
    from("seda:SystemMessageToOwner?concurrentConsumers=5")
            .routeId("SystemMessageToOwner")
            .setHeader(WonCamelConstants.DIRECTION_HEADER, new ConstantURIExpression(URI.create(WONMSG.TYPE_FROM_SYSTEM_STRING)))
            .to("bean:wonMessageIntoCamelProcessor")
            .to("bean:wellformednessChecker")
            .to("bean:signatureChecker")
            .to("bean:wrapperFromSystem")
                    //route to message processing logic
            .to("bean:persister")
            .to("bean:toOwnerSender");


    /**
     * Owner protocol logic: expects messages from OwnerProtocolIn and SystemIntoOwnerProtocol routes.
     */
    from("seda:OwnerProtocolLogic?concurrentConsumers=5")
        //call the default implementation, which may alter the message.
        // Also, it puts any outbound message in the respective header
      .routingSlip(method("messageTypeSlip"))
      .to("bean:persister")
      //swap: outbound becomes 'normal' message, 'normal' becomes 'original' - note: in some cases (create, activate,
      // deactivate) there is no outbound message, hence no 'normal' message after this step.
      .setHeader(WonCamelConstants.ORIGINAL_MESSAGE_HEADER, header(WonCamelConstants.MESSAGE_HEADER))
      .setHeader(WonCamelConstants.MESSAGE_HEADER, header(WonCamelConstants.OUTBOUND_MESSAGE_HEADER))
        //now if the outbound message is one that facet implementations can
        //process, let them do that, then send the resulting message to the remote end.
      .choice()
        .when(PredicateBuilder.and(
                header(WonCamelConstants.MESSAGE_HEADER).isNotNull(),
                new ShouldCallFacetImplForMessagePredicate()))
            //put the local connection URI into the header
            .setHeader(WonCamelConstants.CONNECTION_URI_HEADER,
                    new GetEnvelopePropertyExpression(WonCamelConstants.ORIGINAL_MESSAGE_HEADER,
                            URI.create(WONMSG.SENDER_PROPERTY.getURI().toString())))
            //look into the db to find the facet we are using
            .to("bean:facetExtractor")
            .routingSlip(method("facetTypeSlip"))
            .endChoice() //end choice
      .end()
      .choice()
          .when(
                  //check if the outbound message header is set, otherwise we don't have anything to
                  //send to the remote node
                  //(CAUTION: the actual message is in the default message header).
                  header(WonCamelConstants.OUTBOUND_MESSAGE_HEADER).isNotNull())
            .to("bean:toNodeSender")
      .end()
      //if we didn't raise an exception so far, send a success response
      //for that, we have to re-instate the original (not the outbound) messagge, so we reply to the right one
      .setHeader(WonCamelConstants.MESSAGE_HEADER, header(WonCamelConstants.ORIGINAL_MESSAGE_HEADER))
      .filter(PredicateBuilder.and(
              header(WonCamelConstants.MESSAGE_HEADER).isNotNull(),
              PredicateBuilder.not(new IsResponseMessagePredicate())))
            .to("bean:successResponder");

    /** .to("bean:toNodeSender")
     * Need protocol, incoming
     */
    from("activemq:queue:NeedProtocol.in?concurrentConsumers=5")
       .routeId("WonMessageNodeRoute")
       .setHeader(WonCamelConstants.DIRECTION_HEADER, new ConstantURIExpression(URI.create(WONMSG.TYPE_FROM_EXTERNAL.getURI())))
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
             //put the local connection URI into the header
            .setHeader(WonCamelConstants.CONNECTION_URI_HEADER,
                    new GetEnvelopePropertyExpression(WonCamelConstants.MESSAGE_HEADER,
                            URI.create(WONMSG.RECEIVER_PROPERTY.getURI().toString())))
             //look into the db to find the facet we are using
            .to("bean:facetExtractor")
            //inbound messages are always passed to facet implementations as they
            //are always directed at connections
            .routingSlip(method("facetTypeSlip"))
            //now, we have the message we want to pass on to the owner in the exchange's in header.
            //do we want to send a response back? only if we're not currently processing a respon    
            .to("bean:toOwnerSender")
            .choice()
              .when(PredicateBuilder.and(
                      header(WonCamelConstants.MESSAGE_HEADER).isNotNull(),
                      PredicateBuilder.not(new IsResponseMessagePredicate())))
                .to("bean:successResponder")
            .end();

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
        .when(header(WonCamelConstants.MESSAGE_TYPE_HEADER).isEqualTo(URI.create(WONMSG.TYPE_HINT.getURI().toString())))
          .to("bean:wellformednessChecker")
          .to("bean:signatureChecker")
          .to("bean:wrapperFromExternal")
          .to("bean:hintMessageProcessor?method=process")
          .to("bean:persister")
          .to("bean:toOwnerSender")
        .otherwise()
          .log(LoggingLevel.INFO, "could not route message");

    /**
     * Matcher protocol, outgoing
     */
    from("seda:MatcherProtocolOut?concurrentConsumers=5")
      .routeId("Node2MatcherRoute")
      .to("activemq:topic:MatcherProtocol.Out.Need");


  }

  private class ConstantURIExpression implements Expression
  {
    private URI uri;

    public ConstantURIExpression(final URI uri) {
      this.uri = uri;
    }

    @Override
    public <T> T evaluate(final Exchange exchange, final Class<T> type) {
      return type.cast(uri);
    }
  }

  private class GetEnvelopePropertyExpression implements Expression
  {
    String header;
    URI property;
    private GetEnvelopePropertyExpression(String header, URI property) {
      this.property = property;
      this.header = header;
    }

    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
      WonMessage message = (WonMessage) exchange.getIn().getHeader(header);
      return type.cast(message.getEnvelopePropertyURIValue(property));
    }
  }

}
