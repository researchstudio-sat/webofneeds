package won.node.camel.processor.socket.groupSocket;

import java.lang.invoke.MethodHandles;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.SocketMessageProcessor;
import won.node.camel.service.WonCamelHelper;
import won.protocol.vocabulary.WONMSG;
import won.protocol.vocabulary.WXGROUP;

/**
 * Prevents an echo to the owner.
 * 
 * @author fkleedorfer
 */
@Component
@SocketMessageProcessor(socketType = WXGROUP.GroupSocketString, direction = WONMSG.FromOwnerString, messageType = WONMSG.ConnectionMessageString)
public class SendMessageFromOwnerGroupSocketImpl extends AbstractCamelProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void process(final Exchange exchange) {
        WonCamelHelper.suppressMessageToOwner(exchange);
    }
}
