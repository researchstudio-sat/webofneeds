package won.protocol.exception;

import java.net.URI;

public class IncompatibleSocketsException extends WonProtocolException {
    public IncompatibleSocketsException(URI localSocket, URI targetSocket) {
        super("Incompatible sockets! Local socket: " + localSocket + ", target socket: "
                        + targetSocket);
    }
}
