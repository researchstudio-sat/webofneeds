package won.node.camel.route.fixed;

import static org.apache.camel.builder.PredicateBuilder.isNotEqualTo;
import static org.apache.camel.builder.PredicateBuilder.not;

import java.net.URI;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.ExpressionBuilder;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.jpa.JpaSystemException;

import won.node.camel.predicate.IsResponseMessagePredicate;
import won.node.camel.predicate.ShouldCallSocketImplForMessagePredicate;
import won.node.camel.predicate.ShouldEchoToOwnerPredicate;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim Date: 02.03.2015
 */
public class WonMessageRoutes extends RouteBuilder {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private void logRouteStart(Exchange exchange) {
        // UnitOfWork -> getRouteContext -> Route -> Id.
        String routeId = exchange.getUnitOfWork().getRouteContext().getRoute().getId();
        WonMessage message = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        if (message == null) {
            logger.debug("starting route {}: [no WoNMessage]", routeId);
            return;
        }
        logger.debug("starting route {}: {} type:{}, dir:{}, resp:{}, rem: {}",
                        new String[] { routeId, message.getMessageURI().toString(), message.getMessageType().toString(),
                                        message.getEnvelopeType().toString(),
                                        message.getIsResponseToMessageURI() == null ? "[not a response]"
                                                        : message.getIsResponseToMessageURI().toString(),
                                        message.getCorrespondingRemoteMessageURI() == null ? "[no remote message uri]"
                                                        : message.getCorrespondingRemoteMessageURI().toString() });
    }

    @Override
    public void configure() throws Exception {
        onException(CannotAcquireLockException.class, JpaSystemException.class) // due to our transaction isolation, we
                                                                                // may
                                                                                // have to retry
                        .log(LoggingLevel.INFO,
                                        "re-delivering the current message because of an exception, wonMessage header: ${header."
                                                        + WonCamelConstants.MESSAGE_HEADER + "}")
                        .maximumRedeliveries(2).redeliveryDelay(1000).handled(true)
                        .routeId("retryAfterConcurrentUpdate");
        onException(Exception.class).routeId("onException").handled(true).to("direct:sendFailureResponse");
        // sends the failure response, using the current transaction
        from("direct:sendFailureResponse").routeId("direct:sendFailureResponse").onException(Exception.class).log(
                        "failure during direct:sendFailureResponse, rolling back transaction for exchange ${exchangeId}")
                        .rollback().handled(true).end().transacted("PROPAGATION_REQUIRES_NEW").to("bean:parentLocker")
                        .to("bean:failResponder");
        // sends the success response, using the current transaction
        from("direct:sendSuccessResponse").routeId("direct:sendSuccessResponse").transacted("PROPAGATION_REQUIRES_NEW")
                        .to("bean:parentLocker").to("bean:successResponder"); // --> seda:systemMessageIn or
                                                                              // seda:SystemMessageToOwner
        // checks message, throws exception if something is wrong
        from("direct:checkMessage").routeId("direct:checkMessage").transacted("PROPAGATION_NEVER")
                        .to("bean:wellformednessChecker").to("bean:uriNodePathChecker").to("bean:uriInUseChecker")
                        .to("bean:signatureChecker");
        // adds an envelope and the receivedTimestamp
        from("direct:addEnvelopeAndTimestamp").routeId("direct:addEnvelopeAndTimestamp").to("bean:envelopeAdder")
                        .to("bean:receivedTimestampAdder");
        // in a new transaction, executes the default implementation for each message
        // type, stores the message
        // and sends the response
        from("direct:storeAndRespond").routeId("direct:storeAndRespond").transacted("PROPAGATION_NEVER")
                        // we store the message in a transacted subroute, then send the response
                        .to("direct:executeAndStoreMessage").choice()
                        .when(PredicateBuilder.and(header(WonCamelConstants.MESSAGE_HEADER).isNotNull(),
                                        not(new IsResponseMessagePredicate())))
                        .to("direct:sendSuccessResponse").endChoice().end();
        from("direct:executeAndStoreMessage").transacted("PROPAGATION_REQUIRES_NEW")
                        .routeId("direct:executeAndStoreMessage").to("bean:parentLocker")
                        // remember the connection state (if any)
                        .to("bean:connectionStateChangeBuilder")
                        // call the default implementation, which may alter the message.
                        .routingSlip(method("fixedMessageProcessorSlip"))
                        // depending on connection state change, make/delete derivations
                        .to("bean:dataDeriver")
                        // now persist the message
                        .to("direct:reference-sign-persist");
        // adds message references, signature and persists the message, in the current
        // transaction
        from("direct:reference-sign-persist").errorHandler(noErrorHandler()) // let the exception bubble up
                        .transacted("PROPAGATION_REQUIRES_NEW").routeId("direct:reference-sign-persist")
                        .to("bean:parentLocker").to("bean:referenceAdder").to("bean:signatureAdder")
                        .to("bean:persister");
        from("direct:deleteAtomIfNecessary").errorHandler(noErrorHandler()) // let the exception bubble up
                        .transacted("PROPAGATION_REQUIRES_NEW").routeId("direct:deleteAtomIfNecessary")
                        .to("bean:atomDeleter");
        from("direct:sendToOwner").transacted("PROPAGATION_REQUIRES_NEW").routeId("direct:sendToOwner")
                        // we wait until we obtain a lock on the message's parent
                        // so that we can be sure that processing the message is finished before we send
                        // it to the owner
                        // if we did not do that, the owner may respond before processing the current
                        // message is finished,
                        // which, in case of a connect/open/connectionmesssage sequence may lead to a
                        // failure that is not
                        // expected by the client.
                        .to("bean:parentLocker")
                        // now, we expect the message we want to pass on to the owner in the exchange's
                        // in header.
                        .to("bean:toOwnerSender"); // --> seda:OwnerProtocolOut
        // sends the message to the owner of the connection in the won:sender
        // property.
        // In the case of an outbound message, this is an echo of the message back to
        // the owner, in the
        // case of a system generated outbound message, this copies the message to the
        // owner
        from("direct:echoToOwner").transacted("PROPAGATION_REQUIRES_NEW").routeId("direct:echoToOwner")
                        // we wait until we obtain a lock on the message's parent
                        // so that we can be sure that processing the message is finished before we send
                        // it to the owner
                        // if we did not do that, the owner may respond before processing the current
                        // message is finished,
                        // which, in case of a connect/open/connectionmesssage sequence may lead to a
                        // failure that is not
                        // expected by the client.
                        .to("bean:parentLocker")
                        // now, we expect the message we want to pass on to the owner in the exchange's
                        // in header.
                        .to("bean:toOwnerEchoer"); // --> seda:OwnerProtocolOut
        from("direct:sendToNode").transacted("PROPAGATION_REQUIRES_NEW").routeId("direct:sendToNode")
                        // we wait until we obtain a lock on the message's parent
                        // so that we can be sure that processing the message is finished before we send
                        // it to the remote node
                        // if we did not do that, the other node may respond before processing the
                        // current message is finished,
                        // which, may lead to a failure that is not expected by the client.
                        .to("bean:parentLocker")
                        // now, we expect the message we want to pass on to the node in the exchange's
                        // in header.
                        .to("bean:toNodeSender");
        /**
         * owner protocol, incoming
         */
        from("activemq:queue:OwnerProtocol.in?concurrentConsumers=5").routeId("activemq:queue:OwnerProtocol.in")
                        .transacted("PROPAGATION_NEVER")
                        .setHeader(WonCamelConstants.DIRECTION_HEADER,
                                        new ConstantURIExpression(URI.create(WONMSG.FromOwnerString)))
                        .to("bean:wonMessageIntoCamelProcessor").to("direct:checkMessage")
                        .to("direct:addEnvelopeAndTimestamp").to("bean:directionFromOwnerAdder")
                        // route to msg processing logic
                        .to("direct:OwnerProtocolLogic");
        /**
         * Owner protocol, outgoing. The well-formed, signed message is expected to be
         * in the body.
         */
        from("seda:OwnerProtocolOut?concurrentConsumers=5")
                        // note: here it might happen that the recipient list contains only endpoint
                        // URIs that cannot be found
                        // e.g. when client's public keys changed and hence their queuenames did, too.
                        // Added the onException part to deal
                        // with that.
                        .onException(Exception.class)
                        .log("failure during seda:OwnerProtocolOut, ignoring. Exception message: ${exception.messsage}")
                        .handled(true).stop().end().transacted("PROPAGATION_NEVER").routeId("seda:OwnerProtocolOut")
                        .to("bean:ownerProtocolOutgoingMessagesProcessor").recipientList(header("ownerApplicationIDs"));
        // .to("bean:atomDeleter"); // --> check if the outgoing message is a success
        // response to a delete message so we can delete the atom data
        /**
         * System messages: add timestamp, sign and then process completely
         */
        from("seda:SystemMessageIn?concurrentConsumers=5").routeId("seda:SystemMessageIn")
                        .transacted("PROPAGATION_NEVER").to("bean:wonMessageIntoCamelProcessor")
                        .setHeader(WonCamelConstants.DIRECTION_HEADER,
                                        new ConstantURIExpression(URI.create(WONMSG.FromSystemString)))
                        .to("bean:receivedTimestampAdder")
                        // route to message processing logic
                        .to("direct:OwnerProtocolLogic");
        /**
         * System-to-owner messages: add timestamp, sign but do not process effects etc
         * - just send to owner. Intended for messages that do not atom socket
         * processing (like response messages).
         */
        from("seda:SystemMessageToOwner?concurrentConsumers=5").routeId("seda:SystemMessageToOwner")
                        .transacted("PROPAGATION_NEVER").to("bean:wonMessageIntoCamelProcessor")
                        .setHeader(WonCamelConstants.DIRECTION_HEADER,
                                        new ConstantURIExpression(URI.create(WONMSG.FromSystemString)))
                        // TODO: as soon as messages are signed when they reach this route, perform
                        // signature/wellformedness checks here?
                        .to("bean:receivedTimestampAdder").to("direct:reference-sign-persist")
                        .to("direct:deleteAtomIfNecessary").to("bean:toOwnerSender"); // --> seda:OwnerProtocolOut
        /**
         * Message processing: expects messages from OwnerProtocolIn and SystemMessageIn
         * routes. Messages will be processed, effects executed, and then sent to
         * recipient(s)
         */
        from("direct:OwnerProtocolLogic").routeId("direct:OwnerProtocolLogic")
                        // call the default implementation, which may alter the message.
                        // Also, it puts any outbound message in the respective header
                        .to("direct:storeAndRespond")
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
        from("direct:invokeSocketLogic").transacted("PROPAGATION_REQUIRES_NEW").routeId("direct:invokeSocketLogic")
                        // look into the db to find the socket we are using
                        .to("bean:parentLocker").to("bean:socketExtractor")
                        // find and execute the socket-specific processor
                        .routingSlip(method("socketTypeSlip"));
        from("direct:reactToMessage").transacted("PROPAGATION_REQUIRES_NEW").routeId("direct:reactToMessage")
                        .to("bean:parentLocker").routingSlip(method("fixedMessageReactionProcessorSlip"));
        /**
         * Atom protocol, incoming
         */
        from("activemq:queue:AtomProtocol.in?concurrentConsumers=5").routeId("WonMessageNodeRoute").choice()
                        // (won nodes register the same way as owner applications do)
                        .when(header("methodName").isEqualTo("register"))
                        .to("bean:ownerManagementService?method=registerOwnerApplication")
                        .when(header("methodName").isEqualTo("getEndpoints"))
                        .to("bean:queueManagementService?method=getEndpointsForOwnerApplication").otherwise()
                        .to("bean:wonMessageIntoCamelProcessor")
                        .setHeader(WonCamelConstants.DIRECTION_HEADER,
                                        new ConstantURIExpression(URI.create(WONMSG.FromExternal.getURI())))
                        .to("direct:checkMessage").to("direct:addEnvelopeAndTimestamp")
                        .to("bean:directionFromExternalAdder").to("direct:storeAndRespond")
                        // put the local connection URI into the header
                        .setHeader(WonCamelConstants.CONNECTION_URI_HEADER,
                                        new GetEnvelopePropertyExpression(WonCamelConstants.MESSAGE_HEADER,
                                                        URI.create(WONMSG.recipient.getURI().toString())))
                        .to("direct:invokeSocketLogic").choice()
                        .when(isNotEqualTo(header(WonCamelConstants.SUPPRESS_MESSAGE_TO_OWNER),
                                        ExpressionBuilder.constantExpression(Boolean.TRUE)))
                        .to("direct:sendToOwner").otherwise()
                        .log(LoggingLevel.DEBUG,
                                        "suppressing sending of message to owner because the header '"
                                                        + WonCamelConstants.SUPPRESS_MESSAGE_TO_OWNER + "' is 'true'")
                        .endChoice().end().choice()
                        .when(isNotEqualTo(header(WonCamelConstants.SUPPRESS_MESSAGE_REACTION),
                                        ExpressionBuilder.constantExpression(Boolean.TRUE)))
                        .to("direct:reactToMessage").otherwise()
                        .log(LoggingLevel.DEBUG,
                                        "suppressing sending of message to owner because the header '"
                                                        + WonCamelConstants.SUPPRESS_MESSAGE_TO_OWNER + "' is 'true'")
                        .endChoice().end();
        /**
         * Atom protocol, outgoing We add our signature, but we can't persist the
         * message in this form because its URI says it lives on the recipient node. The
         * recipient will persist it.
         */
        from("seda:AtomProtocolOut?concurrentConsumers=5").routeId("seda:AtomProtocolOut").to("bean:signatureAdder")
                        .to("bean:atomProtocolOutgoingMessagesProcessor");
        /**
         * Matcher protocol, incoming
         */
        from("activemq:queue:MatcherProtocol.in?concurrentConsumers=5").transacted("PROPAGATION_NEVER")
                        .routeId("activemq:queue:MatcherProtocol.in").to("bean:wonMessageIntoCamelProcessor").choice()
                        // we only handle hint messages
                        .when(header(WonCamelConstants.MESSAGE_TYPE_HEADER)
                                        .isEqualTo(URI.create(WONMSG.HintMessage.getURI().toString())))
                        // TODO as soon as Matcher can sign his messages, perform here
                        // .to("bean:wellformednessChecker") and .to("bean:signatureChecker")
                        .to("bean:uriNodePathChecker").to("bean:uriInUseChecker").to("bean:envelopeAdder")
                        .to("bean:directionFromExternalAdder").to("bean:receivedTimestampAdder")
                        .to("direct:processHintAndStore").to("bean:toOwnerSender") // --> seda:OwnerProtocolOut
                        .otherwise().log(LoggingLevel.INFO, "could not route message");
        from("direct:processHintAndStore").transacted("PROPAGATION_REQUIRES_NEW").routeId("direct:processHintAndStore")
                        .to("bean:parentLocker")
                        // call the default implementation, which may alter the message.
                        .to("bean:hintMessageProcessor?method=process").choice()
                        .when(isNotEqualTo(header(WonCamelConstants.IGNORE_HINT),
                                        ExpressionBuilder.constantExpression(Boolean.TRUE)))
                        .to("direct:reference-sign-persist").otherwise()
                        .log(LoggingLevel.DEBUG,
                                        "suppressing sending of message to owner because the header '"
                                                        + WonCamelConstants.IGNORE_HINT + "' is 'true'")
                        .endChoice().end();
        /**
         * Matcher protocol, outgoing
         */
        from("seda:MatcherProtocolOut?concurrentConsumers=5").routeId("seda:MatcherProtocolOut")
                        .transacted("PROPAGATION_NEVER").to("activemq:topic:MatcherProtocol.Out.Atom");
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
