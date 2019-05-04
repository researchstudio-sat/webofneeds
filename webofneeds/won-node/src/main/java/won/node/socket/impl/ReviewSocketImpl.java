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
 * created by MS on 12.12.2018
 */
public class ReviewSocketImpl extends AbstractSocket {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public SocketType getSocketType() {
        return SocketType.ReviewSocket;
    }

    @Override
    public void connectFromAtom(final Connection con, final Model content, final WonMessage wonMessage)
                    throws NoSuchAtomException, IllegalMessageForAtomStateException, ConnectionAlreadyExistsException {
        super.connectFromAtom(con, content, wonMessage);
        /* when connected change linked data */
    }
}
