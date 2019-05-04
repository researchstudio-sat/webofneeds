package won.protocol.exception;

import java.net.URI;

/**
 * User: LEIH-NB Date: 11.11.13
 */
public class NoSuchOwnerApplicationException extends WonProtocolException {
    private URI unknownAtomURI;

    public URI getUnknownAtomURI() {
        return unknownAtomURI;
    }

    public NoSuchOwnerApplicationException() {
        super("owner application not found on this server.");
    }
}
