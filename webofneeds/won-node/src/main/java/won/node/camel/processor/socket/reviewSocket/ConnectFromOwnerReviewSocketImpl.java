package won.node.camel.processor.socket.reviewSocket;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.SocketMessageProcessor;
import won.protocol.vocabulary.WONMSG;
import won.protocol.vocabulary.WXREVIEW;

import java.lang.invoke.MethodHandles;

/**
 * User: MS Date: 12.12.2018
 */
@Component
@SocketMessageProcessor(socketType = WXREVIEW.ReviewSocketString, direction = WONMSG.FromOwnerString, messageType = WONMSG.ConnectMessageString)
public class ConnectFromOwnerReviewSocketImpl extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void process(final Exchange exchange) {
        logger.debug("default socket implementation, not doing anything");
    }
}
