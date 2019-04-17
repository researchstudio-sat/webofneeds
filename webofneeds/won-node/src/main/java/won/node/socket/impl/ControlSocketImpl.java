package won.node.socket.impl;

import won.protocol.model.SocketType;

/**
 * Created with IntelliJ IDEA. User: gabriel Date: 16.09.13 Time: 19:00 To
 * change this template use File | Settings | File Templates.
 */
public class ControlSocketImpl extends AbstractSocket {
    @Override
    public SocketType getSocketType() {
        return SocketType.ControlSocket;
    }
}
