package won.node.camel.processor.fixed;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.protocol.exception.IncompatibleSocketsException;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.Socket;
import won.protocol.repository.ConnectionRepository;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WONMSG;

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

    public void process(Exchange exchange) throws Exception {
        Message message = exchange.getIn();
        WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
        logger.debug("STORING message with id {}", wonMessage.getMessageURI());
        URI recipientAtomURI = wonMessage.getRecipientAtomURI();
        if (isTooManyHints(recipientAtomURI)) {
            exchange.getIn().setHeader(WonCamelConstants.IGNORE_HINT, Boolean.TRUE);
            return;
        }
        Optional<Connection> con = socketHint(wonMessage);
        // build message to send to owner, put in header
        // set the receiver to the newly generated connection uri
        wonMessage.addMessageProperty(WONMSG.recipient, con.get().getConnectionURI());
    }

    public Optional<Connection> socketHint(WonMessage wonMessage) {
        URI recipientAtomURI = wonMessage.getRecipientAtomURIRequired();
        URI recipientWoNNodeURI = wonMessage.getRecipientNodeURIRequired();
        URI targetSocketURI = wonMessage.getHintTargetSocketURIRequired();
        URI recipientSocketURI = wonMessage.getRecipientSocketURIRequired();
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
        if (recipientSocketURI == null) {
            throw new MissingMessagePropertyException(URI.create(WONMSG.recipientSocket.toString()));
        }
        if (!WonLinkedDataUtils.isCompatibleSockets(linkedDataSource, recipientSocketURI, targetSocketURI)) {
            throw new IncompatibleSocketsException(recipientSocketURI, targetSocketURI);
        }
        Socket socket = socketService.getSocket(recipientAtomURI, Optional.ofNullable(recipientSocketURI));
        // create Connection in Database
        Optional<URI> targetAtomURI = WonLinkedDataUtils.getAtomOfSocket(targetSocketURI, linkedDataSource);
        if (!targetAtomURI.isPresent()) {
            throw new IllegalArgumentException("Cannot determine atom of socket " + targetSocketURI);
        }
        Optional<Connection> con = connectionRepository
                        .findOneByAtomURIAndTargetAtomURIAndSocketURIAndTargetSocketURIForUpdate(recipientAtomURI,
                                        targetAtomURI.get(), socket.getSocketURI(), targetSocketURI);
        if (!con.isPresent()) {
            con = Optional.of(connectionService.createConnection(recipientAtomURI, targetAtomURI.get(),
                            recipientSocketURI, socket.getTypeURI(), targetSocketURI, Optional.empty(),
                            ConnectionState.SUGGESTED,
                            ConnectionEventType.MATCHER_HINT));
        }
        return con;
    }

    private boolean isTooManyHints(URI atomURIFromWonMessage) {
        long hintCount = connectionRepository.countByAtomURIAndState(atomURIFromWonMessage, ConnectionState.SUGGESTED);
        return (hintCount > maxSuggestedConnectionCount);
    }
}
