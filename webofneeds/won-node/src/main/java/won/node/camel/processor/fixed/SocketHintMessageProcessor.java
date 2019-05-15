package won.node.camel.processor.fixed;

import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.Socket;
import won.protocol.repository.ConnectionRepository;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.FromExternalString, messageType = WONMSG.SocketHintMessageString)
public class SocketHintMessageProcessor extends AbstractCamelProcessor {
    @Autowired
    private ConnectionRepository connectionRepository;
    @Value("${ignore.hints.suggested.connection.count.max}")
    private Long maxSuggestedConnectionCount = 100L;

    public void process(Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        logger.debug("STORING message with id {}", wonMessage.getMessageURI());
        URI atomURIFromWonMessage = wonMessage.getRecipientAtomURI();
        if (isTooManyHints(atomURIFromWonMessage)) {
            exchange.getIn().setHeader(WonCamelConstants.IGNORE_HINT, Boolean.TRUE);
            return;
        }
        URI wonNodeFromWonMessage = wonMessage.getRecipientNodeURI();
        URI targetSocketURI = wonMessage.getHintTargetSocketURI();
        Double score = wonMessage.getHintScore();
        if (targetSocketURI == null) {
            throw new MissingMessagePropertyException(URI.create(WONMSG.hintTargetSocket.toString()));
        }
        if (wonMessage.getHintTargetAtomURI() != null) {
            throw new IllegalArgumentException("A SocketHintMessage must not have a msg:hintTargetAtom property");
        }
        if (score < 0 || score > 1) {
            throw new IllegalArgumentException("score is not in [0,1]");
        }
        URI wmOriginator = wonMessage.getSenderNodeURI();
        if (wmOriginator == null) {
            throw new IllegalArgumentException("originator is not set");
        }
        URI recipientSocketURI = wonMessage.getRecipientSocketURI();
        if (recipientSocketURI == null) {
            throw new MissingMessagePropertyException(URI.create(WONMSG.recipientSocket.toString()));
        }
        Socket socket = dataService.getSocket(atomURIFromWonMessage, Optional.ofNullable(recipientSocketURI));
        // create Connection in Database
        Optional<Connection> con = connectionRepository
                        .findOneByAtomURIAndTargetAtomURIAndSocketURIAndTargetSocketURIForUpdate(atomURIFromWonMessage,
                                        targetSocketURI, socket.getSocketURI(), targetSocketURI);
        if (!con.isPresent()) {
            URI connectionUri = wonNodeInformationService.generateConnectionURI(wonNodeFromWonMessage);
            con = Optional.of(dataService.createConnection(connectionUri, atomURIFromWonMessage, targetSocketURI, null,
                            socket.getSocketURI(), socket.getTypeURI(), targetSocketURI, ConnectionState.SUGGESTED,
                            ConnectionEventType.MATCHER_HINT));
        }
        // build message to send to owner, put in header
        // set the receiver to the newly generated connection uri
        wonMessage.addMessageProperty(WONMSG.recipient, con.get().getConnectionURI());
    }

    private boolean isTooManyHints(URI atomURIFromWonMessage) {
        long hintCount = connectionRepository.countByAtomURIAndState(atomURIFromWonMessage, ConnectionState.SUGGESTED);
        return (hintCount > maxSuggestedConnectionCount);
    }
}
