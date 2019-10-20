package won.node.camel.route.fixed;

import static org.apache.camel.builder.PredicateBuilder.isNotEqualTo;

import java.lang.invoke.MethodHandles;
import java.net.URI;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.ExpressionBuilder;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.node.camel.predicate.ShouldCallSocketImplForMessagePredicate;
import won.node.camel.predicate.ShouldEchoToOwnerPredicate;
import won.node.camel.predicate.ShouldSuppressReactionPredicate;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.vocabulary.WONMSG;

public class NewWonMessageRoutes extends RouteBuilder {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public NewWonMessageRoutes() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void configure() throws Exception {
        /**
         * Incoming from owner application - OwnerProtocol.in
         */
        from("activemq:queue:OwnerProtocol.in?concurrentConsumers=5").routeId("activemq:queue:OwnerProtocol.in")
                        .transacted("PROPAGATION_REQUIRES_NEW")
                        /* onCompletion: executed after the whole route finishes successfully */
                        .onCompletion()
                        /*
                         * When we're done processing and storing the message and we have sent a
                         * response, we can pass it on to a recipient (if any), and then process any
                         * reactions
                         */
                        // send to recipient (if any)
                        .to("direct:outgoingMessageSender") // todo: implement
                        .choice()
                        /**/.when(PredicateBuilder.not(new ShouldSuppressReactionPredicate()))
                        // react to the message, if we don't want to suppress the reaction, which would
                        // be indicated by a header.
                        /**/.to("direct:messageReactor") // todo:implement
                        /**/.endChoice() // choice
                        /**/.end() // choice
                        .end() // onCompletion
                        /*
                         * end of onCompletion route
                         */
                        /*
                         * start of the main route*
                         */
                        .setHeader(WonCamelConstants.DIRECTION_HEADER,
                                        new ConstantURIExpression(URI.create(WONMSG.FromOwnerString))) // todo maybe we
                                                                                                       // can get rid of
                                                                                                       // this?
                        .to("bean:wonMessageIntoCamelProcessor")
                        .to("direct:checkMessage")
                        // remember the connection state (if any)
                        .to("bean:connectionStateChangeBuilder")
                        // call the default implementation, which may alter the message.
                        .routingSlip(method("fixedMessageProcessorSlip"))
                        // depending on connection state change, make/delete derivations
                        .to("bean:dataDeriver")
                        // now persist the message
                        .to("bean:parentLocker") // TODO do we need that?
                        .choice()
                        /**/.when(new ShouldCallSocketImplForMessagePredicate())
                        /**/.to("direct:invokeSocketLogic")
                        /**/.endChoice()
                        .to("bean:persister")
                        .to("bean:successResponderOut") // create the successresponse message in the exchange's out
                        .to("bean:referenceAdderOut") // add references to the message in the exchange's out
                        .to("bean:signatureAdderOut") // sign the message in the exchange's out
                        .to("bean:persisterOut");
        /**
         * Message processing: expects messages from OwnerProtocolIn and SystemMessageIn
         * routes. Messages will be processed, effects executed, and then sent to
         * recipient(s)
         */
        from("direct:OwnerProtocolLogic").routeId("direct:OwnerProtocolLogic")
                        /*
                         * done // call the default implementation, which may alter the message. //
                         * Also, it puts any outbound message in the respective header
                         * .to("direct:storeAndRespond")
                         */
                        // use the OutboundMessageFactoryProcessor that is expected to be in a header to
                        // create
                        // the outbound message based on the now-saved current message
                        .to("bean:outboundMessageCreator")
                        // remember our original WonMessage, currently in the MESSAGE_HEADER, in the
                        // ORIGINAL_MESSAGE_HEADER
                        .setHeader(WonCamelConstants.ORIGINAL_MESSAGE_HEADER, header(WonCamelConstants.MESSAGE_HEADER))
                        .choice().when(
                                        // we want to send a FROM_SYSTEM message to the owner if it is addressed at the
                                        // owner.
                                        // this is the case if senderURI equals recipientURI and both are non-null.
                                        PredicateBuilder.and(
                                                        header(WonCamelConstants.ORIGINAL_MESSAGE_HEADER).isNotNull(),
                                                        new ShouldEchoToOwnerPredicate()))
                        // swap back: original into MESSAGE_HEADER
                        .setHeader(WonCamelConstants.MESSAGE_HEADER, header(WonCamelConstants.ORIGINAL_MESSAGE_HEADER))
                        // here, we use the echo functionality so a message always gets delivered to the
                        // owner, even if
                        // it is a copy of an outgoing message
                        .to("direct:echoToOwner") // --> seda:OwnerProtocolOut
                        .endChoice().end()
                        // now if the outbound message is one that socket implementations can
                        // process, let them do that, then send the resulting message to the remote end.
                        .choice().when(
                                        // check if the outbound message header is set, otherwise we don't have anything
                                        // to
                                        // send to the remote node
                                        // (CAUTION: the actual message is in the MESSAGE_HEADER and as a backup in the
                                        // ORIGINAL_MESSAGE_HEADER).
                                        header(WonCamelConstants.OUTBOUND_MESSAGE_HEADER).isNotNull())
                        // put outbound message into the MESSAGE_HEADER so the processing chain can use
                        // the normal
                        // header.
                        .setHeader(WonCamelConstants.MESSAGE_HEADER, header(WonCamelConstants.OUTBOUND_MESSAGE_HEADER))
                        .to("direct:sendToNode") // --> seda:AtomProtocolOut
                        .endChoice().end()
                        // if we didn't raise an exception so far, send a success response
                        // for that, we have to re-instate the original (not the outbound) messagge, so
                        // we reply to the right one
                        // this may or may not already have happened
                        .setHeader(WonCamelConstants.MESSAGE_HEADER, header(WonCamelConstants.ORIGINAL_MESSAGE_HEADER))
                        // now, call the socket implementation
                        .choice()
                        .when(PredicateBuilder.and(
                                        header(WonCamelConstants.DIRECTION_HEADER)
                                                        .isEqualTo(URI.create(WONMSG.FromOwnerString)),
                                        PredicateBuilder.and(header(WonCamelConstants.MESSAGE_HEADER).isNotNull(),
                                                        new ShouldCallSocketImplForMessagePredicate())))
                        // put the local connection URI into the header
                        .setHeader(WonCamelConstants.CONNECTION_URI_HEADER,
                                        new GetEnvelopePropertyExpression(WonCamelConstants.ORIGINAL_MESSAGE_HEADER,
                                                        URI.create(WONMSG.sender.getURI().toString())))
                        .to("direct:invokeSocketLogic").endChoice() // end choice
                        .end().choice()
                        .when(isNotEqualTo(header(WonCamelConstants.SUPPRESS_MESSAGE_REACTION),
                                        ExpressionBuilder.constantExpression(Boolean.TRUE)))
                        .to("direct:reactToMessage").otherwise()
                        .log(LoggingLevel.DEBUG,
                                        "suppressing sending of message to owner because the header '"
                                                        + WonCamelConstants.SUPPRESS_MESSAGE_TO_OWNER + "' is 'true'")
                        .endChoice().end();
    }

    private class ConstantURIExpression implements Expression {
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
     * Gets the value of a property inside the message. Only works if a WonMessage
     * object is found in the specified header.
     */
    private class GetEnvelopePropertyExpression implements Expression {
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
