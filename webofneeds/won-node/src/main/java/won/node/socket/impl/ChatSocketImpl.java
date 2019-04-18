package won.node.socket.impl;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForAtomStateException;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.SocketType;

/**
 * Created with IntelliJ IDEA. User: gabriel Date: 16.09.13 Time: 18:42 To
 * change this template use File | Settings | File Templates.
 */
public class ChatSocketImpl extends AbstractSocket {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public SocketType getSocketType() {
        return SocketType.ChatSocket;
    }

    @Override
    public void connectFromAtom(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchAtomException, IllegalMessageForAtomStateException, ConnectionAlreadyExistsException {
        super.connectFromAtom(con, content, wonMessage);
        /* when connected change linked data */
    }
}
