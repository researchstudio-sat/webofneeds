package won.protocol.message.processor.camel;

import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;

import won.protocol.message.WonMessage;
import won.protocol.service.MessageRoutingInfoService;

/**
 * Extracts routing information from the exchange, for the message in the
 * MESSAGE_HEADER.
 * 
 * @author fkleedorfer
 */
public class CamelMessageRoutingInfoService {
    @Autowired
    MessageRoutingInfoService messageRoutingInfoService;

    public URI senderNode(Exchange exchange) {
        return senderNode(exchange, WonCamelConstants.MESSAGE_HEADER)
                        .orElseThrow(() -> new IllegalStateException("Cannot determine senderNode"));
    }

    private Optional<URI> senderNode(Exchange exchange, String header) {
        return getWonMessage(exchange, header)
                        .map(msg -> messageRoutingInfoService.senderNode(msg).orElse(null));
    }

    public URI senderAtom(Exchange exchange) {
        return senderAtom(exchange, WonCamelConstants.MESSAGE_HEADER)
                        .orElseThrow(() -> new IllegalStateException("Cannot determine senderAtom"));
    }

    private Optional<URI> senderAtom(Exchange exchange, String header) {
        return getWonMessage(exchange, header)
                        .map(msg -> messageRoutingInfoService.senderAtom(msg).orElse(null));
    }

    public URI recipientNode(Exchange exchange) {
        return recipientNode(exchange, WonCamelConstants.MESSAGE_HEADER)
                        .orElseThrow(() -> new IllegalStateException("Cannot determine recipientNode"));
    }

    private Optional<URI> recipientNode(Exchange exchange, String header) {
        return getWonMessage(exchange, header)
                        .map(msg -> messageRoutingInfoService.recipientNode(msg).orElse(null));
    }

    public URI recipientAtom(Exchange exchange) {
        return recipientAtom(exchange, WonCamelConstants.MESSAGE_HEADER)
                        .orElseThrow(() -> new IllegalStateException("Cannot determine recipientAtom"));
    }

    private Optional<URI> recipientAtom(Exchange exchange, String header) {
        return getWonMessage(exchange, header)
                        .map(msg -> messageRoutingInfoService.recipientAtom(msg).orElse(null));
    }

    public URI senderSocketType(Exchange exchange) {
        return senderSocketType(exchange, WonCamelConstants.MESSAGE_HEADER)
                        .orElseThrow(() -> new IllegalStateException("Cannot determine senderSocketType"));
    }

    private Optional<URI> senderSocketType(Exchange exchange, String header) {
        return getWonMessage(exchange, header)
                        .map(msg -> messageRoutingInfoService.senderSocketType(msg).orElse(null));
    }

    public URI recipientSocketType(Exchange exchange) {
        return recipientSocketType(exchange, WonCamelConstants.MESSAGE_HEADER)
                        .orElseThrow(() -> new IllegalStateException("Cannot determine recipientSocketType"));
    }

    private Optional<URI> recipientSocketType(Exchange exchange, String header) {
        return getWonMessage(exchange, header)
                        .map(msg -> messageRoutingInfoService.recipientSocketType(msg).orElse(null));
    }

    private Optional<WonMessage> getWonMessage(Exchange exchange, String header) {
        return Optional.ofNullable((WonMessage) exchange.getIn().getHeader(header));
    }
}
