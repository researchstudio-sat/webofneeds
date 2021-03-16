package won.node.camel.processor.general;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.message.WonMessage;
import won.protocol.service.MessageRoutingInfoService;

import java.net.URI;
import java.util.Optional;

import static won.node.camel.service.WonCamelHelper.*;

public class RoutingInfoExtractor implements Processor {
    @Autowired
    MessageRoutingInfoService messageRoutingInfoService;

    @Override
    public void process(Exchange exchange) throws Exception {
        WonMessage message = getMessageRequired(exchange);
        URI atom = messageRoutingInfoService.recipientAtom(message)
                        .orElseThrow(() -> new IllegalArgumentException(
                                        "Cannot determine recipient atom for message " + message.getMessageURI()));
        // the sender node is not there in the case of hint messages.
        Optional<URI> senderNode = messageRoutingInfoService.senderNode(message);
        URI recipientNode = messageRoutingInfoService.recipientNode(message)
                        .orElseThrow(() -> new IllegalArgumentException(
                                        "Cannot determine node for response message "
                                                        + message.getMessageURI()));
        if (senderNode.isPresent()) {
            putSenderNodeURI(exchange, senderNode.get());
        }
        putRecipientNodeURI(exchange, recipientNode);
        putRecipientAtomURI(exchange, atom);
    }
}
