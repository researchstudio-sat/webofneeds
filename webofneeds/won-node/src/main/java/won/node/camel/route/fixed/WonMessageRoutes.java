package won.node.camel.route.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import won.node.camel.predicate.IsResponseMessagePredicate;
import won.node.camel.predicate.ShouldCallFacetImplForMessagePredicate;
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
      .handled(true);



    /**
     * owner protocol, incoming
     */
    from("activemq:queue:OwnerProtocol.in?concurrentConsumers=2")
      .choice()
        .when(header("methodName").isEqualTo("register"))
          .to("bean:ownerManagementService?method=registerOwnerApplication")
        .when(header("methodName").isEqualTo("getEndpoints"))
          .to("bean:queueManagementService?method=getEndpointsForOwnerApplication")
        .otherwise()
          .routeId("WonMessageOwnerRoute")
          .setHeader(WonCamelConstants.DIRECTION_HEADER, new ConstantURIExpression(URI.create(WONMSG.TYPE_FROM_OWNER_STRING)))
          .to("bean:wonMessageIntoCamelProcessor")
          .to("bean:wellformednessChecker")
          .to("bean:uriNodePathChecker")
          .to("bean:uriInUseChecker")
          .to("bean:signatureChecker")
          .to("bean:envelopeAdder")
          .to("bean:directionFromOwnerAdder")
          .to("bean:receivedTimestampAdder")
          //route to msg processing logic
          .to("seda:OwnerProtocolLogic");

    /**
     * Owner protocol, outgoing.
     * The well-formed, signed message is expected to be in the body.
     */
    from("seda:OwnerProtocolOut?concurrentConsumers=2")
      .routeId("OwnerProtocolOut")
      .to("bean:ownerProtocolOutgoingMessagesProcessor")
      //note: here it might happen that the recipient list contains only endpoint URIs that cannot be found
      // e.g. when client's public keys changed and hence their queuenames did, too. Added stopOnException to deal
      // with that.
      .recipientList(header("ownerApplicationIDs")).stopOnException();

    /**
     * System messages: treated almost as incoming from owner.
     */
    from("seda:SystemMessageIn?concurrentConsumers=2")
      .to("bean:wonMessageIntoCamelProcessor")
      .routeId("SystemMessageIn")
      .setHeader(WonCamelConstants.DIRECTION_HEADER, new ConstantURIExpression(URI.create(WONMSG.TYPE_FROM_SYSTEM_STRING)))
      .to("bean:receivedTimestampAdder")
      //route to message processing logic
      .to("seda:OwnerProtocolLogic");

    /**
     * System-to-owner messages: treated almost as incoming from remote node.
     * Intended for messages that do not need facet processing (like response messages).
     */
    from("seda:SystemMessageToOwner?concurrentConsumers=2")
      .to("bean:wonMessageIntoCamelProcessor")
      .routeId("SystemMessageToOwner")
      .setHeader(WonCamelConstants.DIRECTION_HEADER, new ConstantURIExpression(URI.create(WONMSG.TYPE_FROM_SYSTEM_STRING)))
        //TODO: as soon as messages are signed when they reach this route, perform signature/wellformedness checks here?
      .to("bean:receivedTimestampAdder")
      .to("bean:referenceAdder")
      .to("bean:signatureAdder")
              //route to message processing logic
      .to("bean:persister")
      .to("bean:toOwnerSender");


    /**
     * Owner protocol logic: expects messages from OwnerProtocolIn and SystemIntoOwnerProtocol routes.
     */
    from("seda:OwnerProtocolLogic?concurrentConsumers=2")
        //call the default implementation, which may alter the message.
        // Also, it puts any outbound message in the respective header
      .routeId("OwnerProtocolLogic")
      .routingSlip(method("fixedMessageProcessorSlip"))
      .to("bean:referenceAdder")
      .to("bean:signatureAdder")
      .to("bean:persister")
      //swap: outbound becomes 'normal' message, 'normal' becomes 'original' - note: in some cases (create, activate,
      // deactivate) there is no outbound message, hence no 'normal' message after this step.
      .setHeader(WonCamelConstants.ORIGINAL_MESSAGE_HEADER, header(WonCamelConstants.MESSAGE_HEADER))
      .setHeader(WonCamelConstants.MESSAGE_HEADER, header(WonCamelConstants.OUTBOUND_MESSAGE_HEADER))
        //now if the outbound message is one that facet implementations can
        //process, let them do that, then send the resulting message to the remote end.

      .choice()
          .when(
            //check if the outbound message header is set, otherwise we don't have anything to
            //send to the remote node
            //(CAUTION: the actual message is in the default message header).
            header(WonCamelConstants.OUTBOUND_MESSAGE_HEADER).isNotNull())
            //swap back: outbound into normal
            .setHeader(WonCamelConstants.MESSAGE_HEADER, header(WonCamelConstants.OUTBOUND_MESSAGE_HEADER))
            .to("bean:toNodeSender")
            .endChoice()
       .end()
       .choice()
          .when(
            //check if the direction of the original message is 'FromSystem'.
            //If this is the case, we want to send it to the owner (so they know it happened)
            // BUT: we don't do this for response messages: the owner does not need to know we sent
            // a response message to the remote end.
            PredicateBuilder.and(
              PredicateBuilder.and(
                header(WonCamelConstants.MESSAGE_HEADER).isNotNull(),
                PredicateBuilder.not(new IsResponseMessagePredicate())),
                header(WonCamelConstants.DIRECTION_HEADER).isEqualTo(WONMSG.TYPE_FROM_SYSTEM_STRING)
            ))
            //swap back: original into normal
            .setHeader(WonCamelConstants.MESSAGE_HEADER, header(WonCamelConstants.ORIGINAL_MESSAGE_HEADER))
            .to("bean:toOwnerSender")
            .endChoice()
      .end()

      //if we didn't raise an exception so far, send a success response
      //for that, we have to re-instate the original (not the outbound) messagge, so we reply to the right one
      .setHeader(WonCamelConstants.MESSAGE_HEADER, header(WonCamelConstants.ORIGINAL_MESSAGE_HEADER))
      .choice()
        .when(PredicateBuilder.and(
              header(WonCamelConstants.MESSAGE_HEADER).isNotNull(),
              PredicateBuilder.not(new IsResponseMessagePredicate())))
            .to("bean:successResponder")
        .endChoice()
        .end()
      //now, call the facet implementation
      .choice()
        .when(PredicateBuilder.and(
          header(WonCamelConstants.DIRECTION_HEADER).isEqualTo(URI.create(WONMSG.TYPE_FROM_OWNER_STRING)),
          PredicateBuilder.and(
            header(WonCamelConstants.MESSAGE_HEADER).isNotNull(),
            new ShouldCallFacetImplForMessagePredicate())))
        //put the local connection URI into the header
        .setHeader(WonCamelConstants.CONNECTION_URI_HEADER,
                   new GetEnvelopePropertyExpression(WonCamelConstants.ORIGINAL_MESSAGE_HEADER,
                                                     URI.create(WONMSG.SENDER_PROPERTY.getURI().toString())))
        //look into the db to find the facet we are using
        .to("bean:facetExtractor")
        .routingSlip(method("facetTypeSlip"))
        .endChoice() //end choice
        .end()
      .routingSlip(method("fixedMessageReactionProcessorSlip"));

    /**
     * Need protocol, incoming
     */
    from("activemq:queue:NeedProtocol.in?concurrentConsumers=2")
       .routeId("WonMessageNodeRoute")
       .choice()
            // (won nodes register the same way as owner applications do)
         .when(header("methodName").isEqualTo("register"))
          .to("bean:ownerManagementService?method=registerOwnerApplication")
         .when(header("methodName").isEqualTo("getEndpoints"))
          .to("bean:queueManagementService?method=getEndpointsForOwnerApplication")
         .otherwise()
            .to("bean:wonMessageIntoCamelProcessor")
            .setHeader(WonCamelConstants.DIRECTION_HEADER, new ConstantURIExpression(URI.create(WONMSG.TYPE_FROM_EXTERNAL.getURI())))
            .to("bean:wellformednessChecker")
            .to("bean:uriNodePathChecker")
            .to("bean:uriInUseChecker")
            .to("bean:signatureChecker")
            .to("bean:envelopeAdder")
            .to("bean:directionFromExternalAdder")
            .to("bean:receivedTimestampAdder")
            //call the default implementation, which may alter the message.
            .routingSlip(method("fixedMessageProcessorSlip"))
            .to("bean:referenceAdder")
            .to("bean:signatureAdder")
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
            .endChoice()
            .end()
    .routingSlip(method("fixedMessageReactionProcessorSlip"));

    /**
     * Need protocol, outgoing
     * We add our signature, but we can't persist the message in this form
     * because its URI says it lives on the recipient node. The recipient will persist it.
     */
    from("seda:NeedProtocolOut?concurrentConsumers=2").routeId("Node2NodeRoute")
            .to("bean:signatureAdder")
            .to("bean:needProtocolOutgoingMessagesProcessor");

    /**
     * Matcher protocol, incoming
     */
    from("activemq:queue:MatcherProtocol.in?concurrentConsumers=2")
      .to("bean:wonMessageIntoCamelProcessor")
      .routeId("WonMessageMatcherRoute")
      .choice()
        //we only handle hint messages
        .when(header(WonCamelConstants.MESSAGE_TYPE_HEADER).isEqualTo(URI.create(WONMSG.TYPE_HINT.getURI().toString())))
          //TODO as soon as Matcher can sign his messages, perform here .to("bean:wellformednessChecker") and .to("bean:signatureChecker")
          .to("bean:uriNodePathChecker")
          .to("bean:uriInUseChecker")
          .to("bean:envelopeAdder")
          .to("bean:directionFromExternalAdder")
          .to("bean:receivedTimestampAdder")
          .to("bean:hintMessageProcessor?method=process")
          .to("bean:referenceAdder")
          .to("bean:signatureAdder")
          .to("bean:persister")
          .to("bean:toOwnerSender")
        .otherwise()
          .log(LoggingLevel.INFO, "could not route message");

    /**
     * Matcher protocol, outgoing
     */
    from("seda:MatcherProtocolOut?concurrentConsumers=2")
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

  /**
   * Gets the value of a property inside the message. Only works if
   * a WonMessage object is found in the specified header.
   */
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
