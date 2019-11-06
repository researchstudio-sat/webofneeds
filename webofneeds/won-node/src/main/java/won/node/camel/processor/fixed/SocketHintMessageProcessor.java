package won.node.camel.processor.fixed;

import static won.node.camel.processor.WonCamelHelper.*;

import java.lang.invoke.MethodHandles;
import java.net.URI;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.WonCamelHelper;
import won.node.service.persistence.ConnectionService;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.repository.ConnectionRepository;

/**
 * User: syim Date: 02.03.2015
 */
@Component
public class SocketHintMessageProcessor extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private ConnectionRepository connectionRepository;
    @Value("${ignore.hints.suggested.connection.count.max}")
    private Long maxSuggestedConnectionCount = 100L;
    @Autowired
    private ConnectionService connectionService;

    public void process(Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = getMessageRequired(exchange);
        logger.debug("STORING message with id {}", wonMessage.getMessageURI());
        URI recipientAtomURI = wonMessage.getRecipientAtomURI();
        if (isTooManyHints(recipientAtomURI)) {
            exchange.getIn().setHeader(WonCamelConstants.IGNORE_HINT_HEADER, Boolean.TRUE);
            return;
        }
        Connection con = connectionService.socketHint(wonMessage);
        WonCamelHelper.putParentURI(exchange, con.getConnectionURI());
    }

    private boolean isTooManyHints(URI atomURIFromWonMessage) {
        long hintCount = connectionRepository.countByAtomURIAndState(atomURIFromWonMessage, ConnectionState.SUGGESTED);
        return (hintCount > maxSuggestedConnectionCount);
    }
}
