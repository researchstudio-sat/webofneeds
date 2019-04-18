package won.protocol.exception;

import java.net.URI;

public class IncompatibleSocketTypesException extends WonProtocolException {
    public IncompatibleSocketTypesException(URI localSocket, URI localSocketType, URI targetSocket,
                    URI targetSocketType) {
        super("Incompatible sockets! Local socket: " + localSocket + " (type: " + localSocketType + "), remote socket: "
                        + targetSocket + " (type: " + targetSocketType + ")");
    }
}
