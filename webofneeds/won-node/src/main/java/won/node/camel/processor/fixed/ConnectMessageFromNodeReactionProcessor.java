package won.node.camel.processor.fixed;

import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageReactionProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.Socket;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WONMSG;

@Component
@FixedMessageReactionProcessor(direction = WONMSG.FromExternalString, messageType = WONMSG.ConnectMessageString)
public class ConnectMessageFromNodeReactionProcessor extends AbstractCamelProcessor {
    Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void process(Exchange exchange) throws Exception {
        // if the connection's socket isAutoOpen, send an open automatically.
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        Optional<URI> atomURI = Optional.of(wonMessage.getRecipientAtomURI());
        Optional<URI> connectionURI = Optional.of(wonMessage.getRecipientURI());
        if (connectionURI.isPresent() && atomURI.isPresent()) {
            Optional<Connection> con = connectionRepository.findOneByConnectionURIForUpdate(connectionURI.get());
            if (con.isPresent()) {
                Socket socket = socketRepository.findOneBySocketURI(con.get().getSocketURI());
                Optional<URI> targetSocket = WonLinkedDataUtils.getTypeOfSocket(con.get().getTargetSocketURI(),
                                linkedDataSource);
                if (targetSocket.isPresent() && socketService.isAutoOpen(socket.getTypeURI(), targetSocket.get())) {
                    sendAutoOpenForConnect(wonMessage);
                }
            }
        }
    }

    private void sendAutoOpenForConnect(WonMessage connectMessageToReactTo) {
        URI fromWonNodeURI = connectMessageToReactTo.getRecipientNodeURI();
        URI messageURI = wonNodeInformationService.generateEventURI(fromWonNodeURI);
        WonMessage msg = WonMessageBuilder
                        .setMessagePropertiesForOpen(messageURI, connectMessageToReactTo,
                                        "Connection request accepted automatically by WoN node")
                        .setWonMessageDirection(WonMessageDirection.FROM_SYSTEM).build();
        logger.info("sending auto-open for connection {}, reacting to connect", msg.getSenderURI());
        super.sendSystemMessage(msg);
    }
}
