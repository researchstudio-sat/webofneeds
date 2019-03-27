package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractFromOwnerCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.vocabulary.WONMSG;

/**
 * Required for auto-open facets. Delegates the processing method to the
 * OpenMessageFromOwnerProcessor.
 */
@Component
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_SYSTEM_STRING, messageType = WONMSG.TYPE_OPEN_STRING)
public class OpenMessageFromSystemProcessor extends AbstractFromOwnerCamelProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private OpenMessageFromOwnerProcessor openFromOwnerProcessor;

    @Override
    public void process(Exchange exchange) throws Exception {
        logger.info("processing OPEN message from system for {} as if it was sent from owner",
                        ((WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER)).getSenderURI());
        openFromOwnerProcessor.process(exchange);
    }

    public void setOpenFromOwnerProcessor(OpenMessageFromOwnerProcessor openFromOwnerProcessor) {
        this.openFromOwnerProcessor = openFromOwnerProcessor;
    }
}
