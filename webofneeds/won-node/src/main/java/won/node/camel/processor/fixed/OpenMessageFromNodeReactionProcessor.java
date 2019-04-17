package won.node.camel.processor.fixed;

import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageReactionProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.Socket;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WONMSG;

@Component
@FixedMessageReactionProcessor(direction = WONMSG.FromExternalString, messageType = WONMSG.OpenMessageString)
public class OpenMessageFromNodeReactionProcessor extends AbstractCamelProcessor {
    @Override
    public void process(Exchange exchange) throws Exception {
        // if the connection's socket isAutoOpen and the connection state is
        // REQUEST_RECEIVED
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        Optional<URI> atomURI = Optional.of(wonMessage.getRecipientAtomURI());
        Optional<URI> connectionURI = Optional.of(wonMessage.getRecipientURI());
        if (connectionURI.isPresent() && atomURI.isPresent()) {
            Optional<Connection> con = connectionRepository.findOneByConnectionURIForUpdate(connectionURI.get());
            if (con.isPresent() && con.get().getState() == ConnectionState.REQUEST_RECEIVED) {
                Socket socket = socketRepository.findOneBySocketURI(con.get().getSocketURI());
                Optional<URI> targetSocket = WonLinkedDataUtils.getTypeOfSocket(con.get().getTargetSocketURI(),
                                linkedDataSource);
                if (targetSocket.isPresent() && socketService.isAutoOpen(socket.getTypeURI(), targetSocket.get())) {
                    sendAutoOpenForOpen(wonMessage);
                }
            }
        }
    }

    private void sendAutoOpenForOpen(WonMessage connectMessageToReactTo) {
        URI fromWonNodeURI = connectMessageToReactTo.getRecipientNodeURI();
        URI messageURI = wonNodeInformationService.generateEventURI(fromWonNodeURI);
        WonMessage msg = WonMessageBuilder
                        .setMessagePropertiesForOpen(messageURI, connectMessageToReactTo,
                                        "This is an automatic OPEN message sent by the WoN node")
                        .setWonMessageDirection(WonMessageDirection.FROM_SYSTEM).build();
        logger.info("sending auto-open for connection {}, reacting to open", msg.getSenderURI());
        super.sendSystemMessage(msg);
    }
}
