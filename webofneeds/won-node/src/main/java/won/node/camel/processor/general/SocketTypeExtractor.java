package won.node.camel.processor.general;

import static won.node.camel.processor.WonCamelHelper.*;

import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;

import won.node.service.persistence.SocketService;
import won.protocol.exception.NoSuchSocketException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.model.Socket;

public class SocketTypeExtractor implements Processor {
    @Autowired
    SocketService socketService;

    @Override
    public void process(Exchange exchange) throws Exception {
        WonMessage msg = getMessageRequired(exchange);
        WonMessageDirection direction = getDirectionRequired(exchange);
        Optional<URI> socketURI = Optional.empty();
        if (direction.isFromExternal()) {
            socketURI = Optional.ofNullable(msg.getRecipientSocketURI());
        } else {
            socketURI = Optional.ofNullable(msg.getSenderSocketURI());
        }
        if (socketURI.isPresent()) {
            Optional<Socket> socket = socketService.getSocket(socketURI.get());
            if (socket.isPresent()) {
                putSocketTypeURI(exchange, socket.get().getTypeURI());
            } else {
                throw new NoSuchSocketException(socketURI.get());
            }
        }
    }
}
