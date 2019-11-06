package won.node.camel.processor.general;

import static won.node.camel.processor.WonCamelHelper.*;

import java.net.URI;
import java.util.Objects;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;

import won.protocol.message.WonMessage;
import won.protocol.service.MessageRoutingInfoService;

public class ResponseRoutingInfoExtractor implements Processor {
    @Autowired
    MessageRoutingInfoService messageRoutingInfoService;

    @Override
    public void process(Exchange exchange) throws Exception {
        WonMessage response = getResponseRequired(exchange);
        Objects.requireNonNull(response);
        URI atom = messageRoutingInfoService.recipientAtom(response)
                        .orElseThrow(() -> new IllegalArgumentException(
                                        "Cannot dertermine recipient atom for response message "
                                                        + response.getMessageURI()));
        URI senderNode = messageRoutingInfoService.senderNode(response)
                        .orElseThrow(() -> new IllegalArgumentException(
                                        "Cannot dertermine sender node for response message "
                                                        + response.getMessageURI()));
        URI recipientNode = messageRoutingInfoService.recipientNode(response)
                        .orElseThrow(() -> new IllegalArgumentException(
                                        "Cannot dertermine recipient node for response message "
                                                        + response.getMessageURI()));
        putRecipientAtomURI(exchange, atom);
        putSenderNodeURI(exchange, senderNode);
        putRecipientNodeURI(exchange, recipientNode);
    }
}
