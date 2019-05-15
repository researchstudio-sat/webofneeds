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
@FixedMessageProcessor(direction = WONMSG.FromExternalString, messageType = WONMSG.HintMessageString)
public class HintMessageProcessor extends AbstractCamelProcessor {
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
        URI otherAtomURIFromWonMessage = URI.create(RdfUtils.findOnePropertyFromResource(wonMessage.getMessageContent(),
                        wonMessage.getMessageURI(), WON.matchCounterpart).asResource().getURI());
        double wmScore = RdfUtils.findOnePropertyFromResource(wonMessage.getMessageContent(),
                        wonMessage.getMessageURI(), WON.matchScore).asLiteral().getDouble();
        URI wmOriginator = wonMessage.getSenderNodeURI();
        if (wmScore < 0 || wmScore > 1)
            throw new IllegalArgumentException("score is not in [0,1]");
        if (wmOriginator == null)
            throw new IllegalArgumentException("originator is not set");
        // socket: either specified or default
        URI socketURI = WonRdfUtils.SocketUtils.getSocket(wonMessage);
        // remote socket: either specified or null
        Optional<URI> targetSocketURI = Optional.ofNullable(WonRdfUtils.SocketUtils.getTargetSocket(wonMessage));
        Socket socket = dataService.getSocket(atomURIFromWonMessage,
                        socketURI == null ? Optional.empty() : Optional.of(socketURI));
        // create Connection in Database
        Optional<Connection> con = Optional.empty();
        if (targetSocketURI.isPresent()) {
            con = connectionRepository.findOneByAtomURIAndTargetAtomURIAndSocketURIAndTargetSocketURIForUpdate(
                            atomURIFromWonMessage, otherAtomURIFromWonMessage, socket.getSocketURI(),
                            targetSocketURI.get());
        } else {
            con = connectionRepository.findOneByAtomURIAndTargetAtomURIAndSocketURIAndNullTargetSocketForUpdate(
                            atomURIFromWonMessage, otherAtomURIFromWonMessage, socket.getSocketURI());
        }
        if (!con.isPresent()) {
            URI connectionUri = wonNodeInformationService.generateConnectionURI(wonNodeFromWonMessage);
            con = Optional.of(dataService.createConnection(connectionUri, atomURIFromWonMessage,
                            otherAtomURIFromWonMessage, null, socket.getSocketURI(), socket.getTypeURI(),
                            targetSocketURI.orElse(null), ConnectionState.SUGGESTED, ConnectionEventType.MATCHER_HINT));
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
