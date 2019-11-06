package won.node.camel.processor;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.camel.Exchange;

import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.util.RdfUtils;

public class WonCamelHelper {
    //// message
    public static void putMessage(Exchange exchange, WonMessage message) {
        Objects.requireNonNull(exchange);
        Objects.requireNonNull(message);
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, message);
    }

    public static Optional<WonMessage> getMessage(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return Optional.ofNullable((WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER));
    }

    public static WonMessage getMessageRequired(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return getMessage(exchange).orElseThrow(expectedHeader(WonCamelConstants.MESSAGE_HEADER));
    }

    //// response
    public static void putResponse(Exchange exchange, WonMessage response) {
        Objects.requireNonNull(exchange);
        Objects.requireNonNull(response);
        exchange.getIn().setHeader(WonCamelConstants.RESPONSE_HEADER, response);
    }

    public static Optional<WonMessage> getResponse(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return Optional.ofNullable((WonMessage) exchange.getIn().getHeader(WonCamelConstants.RESPONSE_HEADER));
    }

    public static WonMessage getResponseRequired(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return getResponse(exchange).orElseThrow(expectedHeader(WonCamelConstants.RESPONSE_HEADER));
    }

    public static void removeResponse(Exchange exchange) {
        Objects.requireNonNull(exchange);
        exchange.getIn().removeHeader(WonCamelConstants.RESPONSE_HEADER);
    }

    //// messageToNode
    public static void putMessageToSend(Exchange exchange, WonMessage response) {
        Objects.requireNonNull(exchange);
        Objects.requireNonNull(response);
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TO_SEND_HEADER, response);
    }

    public static Optional<WonMessage> getMessageToSend(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return Optional.ofNullable((WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_TO_SEND_HEADER));
    }

    public static WonMessage getMessageToSendRequired(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return getMessageToSend(exchange).orElseThrow(expectedHeader(WonCamelConstants.MESSAGE_TO_SEND_HEADER));
    }

    public static void removeMessageToSend(Exchange exchange) {
        Objects.requireNonNull(exchange);
        exchange.getIn().removeHeader(WonCamelConstants.MESSAGE_TO_SEND_HEADER);
    }

    //// direction
    public static Optional<WonMessageDirection> getDirection(Exchange exchange) {
        Objects.requireNonNull(exchange);
        URI uri = (URI) exchange.getIn().getHeader(WonCamelConstants.DIRECTION_HEADER);
        if (uri == null)
            return Optional.empty();
        WonMessageDirection direction = WonMessageDirection.getWonMessageDirection(uri);
        return Optional.ofNullable(direction);
    }

    public static WonMessageDirection getDirectionRequired(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return getDirection(exchange).orElseThrow(expectedHeader(WonCamelConstants.DIRECTION_HEADER));
    }

    public static void putDirection(Exchange exchange, WonMessageDirection direction) {
        Objects.requireNonNull(direction);
        Objects.requireNonNull(exchange);
        exchange.getIn().setHeader(WonCamelConstants.DIRECTION_HEADER, URI.create(direction.getResource().toString()));
    }

    //// type
    public static Optional<WonMessageType> getMessageType(Exchange exchange) {
        Objects.requireNonNull(exchange);
        URI uri = (URI) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_TYPE_HEADER);
        if (uri == null)
            return Optional.empty();
        WonMessageType type = WonMessageType.getWonMessageType(uri);
        return Optional.ofNullable(type);
    }

    public static WonMessageType getMessageTypeRequired(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return getMessageType(exchange).orElseThrow(expectedHeader(WonCamelConstants.MESSAGE_TYPE_HEADER));
    }

    public static void putMessageType(Exchange exchange, WonMessageType type) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(exchange);
        exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER, URI.create(type.getResource().toString()));
    }

    //// connectionURI
    public static void putConnectionURI(Exchange exchange, URI connectionURI) {
        Objects.requireNonNull(connectionURI);
        Objects.requireNonNull(exchange);
        exchange.getIn().setHeader(WonCamelConstants.CONNECTION_URI_HEADER, connectionURI);
    }

    public static Optional<URI> getConnectionURI(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return Optional.ofNullable((URI) exchange.getIn().getHeader(WonCamelConstants.CONNECTION_URI_HEADER));
    }

    public static URI getConnectionURIRequired(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return getConnectionURI(exchange).orElseThrow(expectedHeader(WonCamelConstants.CONNECTION_URI_HEADER));
    }

    //// socketTypeURI
    public static void putSocketTypeURI(Exchange exchange, URI socketTypeURI) {
        Objects.requireNonNull(socketTypeURI);
        Objects.requireNonNull(exchange);
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_HEADER, socketTypeURI);
    }

    public static Optional<URI> getSocketTypeURI(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return Optional.ofNullable((URI) exchange.getIn().getHeader(WonCamelConstants.SOCKET_TYPE_HEADER));
    }

    public static URI getSocketTypeURIRequired(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return getSocketTypeURI(exchange).orElseThrow(expectedHeader(WonCamelConstants.SOCKET_TYPE_HEADER));
    }

    //// senderNode
    public static void putSenderNodeURI(Exchange exchange, URI message) {
        Objects.requireNonNull(exchange);
        Objects.requireNonNull(message);
        exchange.getIn().setHeader(WonCamelConstants.SENDER_NODE_HEADER, message);
    }

    public static Optional<URI> getSenderNodeURI(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return Optional.ofNullable((URI) exchange.getIn().getHeader(WonCamelConstants.SENDER_NODE_HEADER));
    }

    public static URI getSenderNodeURIRequired(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return getSenderNodeURI(exchange).orElseThrow(expectedHeader(WonCamelConstants.SENDER_NODE_HEADER));
    }

    //// recipientNode
    public static void putRecipientNodeURI(Exchange exchange, URI message) {
        Objects.requireNonNull(exchange);
        Objects.requireNonNull(message);
        exchange.getIn().setHeader(WonCamelConstants.RECIPIENT_NODE_HEADER, message);
    }

    public static Optional<URI> getRecipientNodeURI(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return Optional.ofNullable((URI) exchange.getIn().getHeader(WonCamelConstants.RECIPIENT_NODE_HEADER));
    }

    public static URI getRecipientNodeURIRequired(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return getRecipientNodeURI(exchange).orElseThrow(expectedHeader(WonCamelConstants.RECIPIENT_NODE_HEADER));
    }

    //// atom
    public static void putRecipientAtomURI(Exchange exchange, URI message) {
        Objects.requireNonNull(exchange);
        Objects.requireNonNull(message);
        exchange.getIn().setHeader(WonCamelConstants.RECIPIENT_ATOM_HEADER, message);
    }

    public static Optional<URI> getRecipientAtomURI(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return Optional.ofNullable((URI) exchange.getIn().getHeader(WonCamelConstants.RECIPIENT_ATOM_HEADER));
    }

    public static URI getRecipientAtomURIRequired(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return getRecipientAtomURI(exchange).orElseThrow(expectedHeader(WonCamelConstants.RECIPIENT_ATOM_HEADER));
    }

    //// suppress
    public static void suppressMessageToNode(Exchange exchange) {
        Objects.requireNonNull(exchange);
        exchange.getIn().setHeader(WonCamelConstants.SUPPRESS_MESSAGE_TO_NODE_HEADER, true);
    }

    //// parent
    public static void putParentURI(Exchange exchange, URI parentURI) {
        Objects.requireNonNull(parentURI);
        Objects.requireNonNull(exchange);
        exchange.getIn().setHeader(WonCamelConstants.PARENT_URI_HEADER, parentURI);
    }

    public static Optional<URI> getParentURI(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return Optional.ofNullable((URI) exchange.getIn().getHeader(WonCamelConstants.PARENT_URI_HEADER));
    }

    public static URI getParentURIRequired(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return getParentURI(exchange).orElseThrow(expectedHeader(WonCamelConstants.PARENT_URI_HEADER));
    }

    public static void removeParentURI(Exchange exchange) {
        Objects.requireNonNull(exchange);
        exchange.getIn().removeHeader(WonCamelConstants.PARENT_URI_HEADER);
    }

    //// owner app id
    public static Optional<String> getOwnerApplicationId(Exchange exchange) {
        Objects.requireNonNull(exchange);
        return Optional.ofNullable((String) exchange.getIn().getHeader(WonCamelConstants.OWNER_APPLICATION_ID_HEADER));
    }

    public static String getOwnerApplicationIdRequired(Exchange exchange) {
        return getOwnerApplicationId(exchange)
                        .orElseThrow(expectedHeader(WonCamelConstants.OWNER_APPLICATION_ID_HEADER));
    }

    //// msg from body
    public static Optional<WonMessage> getMessageFromBody(Exchange exchange) {
        String datasetAsString = (String) exchange.getIn().getBody();
        if (datasetAsString == null) {
            return Optional.empty();
        }
        return Optional.of(WonMessage.of(RdfUtils.readDatasetFromString(datasetAsString,
                        WonCamelConstants.RDF_LANGUAGE_FOR_MESSAGE)));
    }

    public static void putMessageIntoBody(Exchange exchange, WonMessage message) {
        exchange.getIn().setBody(WonMessageEncoder.encode(message, WonCamelConstants.RDF_LANGUAGE_FOR_MESSAGE));
    }

    public static WonMessage getMessageFromBodyRequired(Exchange exchange) {
        return getMessageFromBody(exchange).orElseThrow(() -> new IllegalStateException(
                        "Expected to find a serialized WonMessage in the body of the camel exchange's In message"));
    }

    public static void stopExchange(Exchange exchange) {
        exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
    }

    /********************************************************
     * Private utility methods
     ********************************************************/
    private static Supplier<RuntimeException> expectedHeader(String header) {
        Objects.requireNonNull(header);
        return () -> new IllegalStateException(
                        "Expected to find a '" + header + "' header in camel exchange");
    }
}
