package won.node.camel.processor.general;

import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;

import won.node.camel.processor.WonCamelHelper;
import won.node.service.persistence.MessageService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;

public class ParentFinder implements Processor {
    @Autowired
    MessageService messageService;

    @Override
    public void process(Exchange exchange) throws Exception {
        if (WonCamelHelper.getParentURI(exchange).isPresent()) {
            // we're done - we found the parent before
            return;
        }
        WonMessageDirection direction = WonCamelHelper.getDirectionRequired(exchange);
        WonMessage msg = WonCamelHelper.getMessageRequired(exchange);
        Optional<URI> parentURI = Optional.empty();
        parentURI = messageService.getParentofMessage(msg, direction);
        parentURI.ifPresent(uri -> WonCamelHelper.putParentURI(exchange, uri));
    }
}
