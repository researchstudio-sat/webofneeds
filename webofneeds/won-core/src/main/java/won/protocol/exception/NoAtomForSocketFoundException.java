package won.protocol.exception;

import java.net.URI;

public class NoAtomForSocketFoundException extends WonProtocolException {
    public NoAtomForSocketFoundException(URI socketURI) {
        super("No atom found for socket " + socketURI);
    }
}
