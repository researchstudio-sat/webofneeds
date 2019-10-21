package won.protocol.exception;

import java.net.URI;
import java.text.MessageFormat;

/**
 * User: LEIH-NB Date: 20.02.14
 */
public class NoSuchSocketException extends RuntimeException {
    private URI unknownSocketURI;

    public URI getUnknownAtomURI() {
        return unknownSocketURI;
    }

    public NoSuchSocketException(final URI unknownSocketURI) {
        super(MessageFormat.format("No atom with the URI {0} is known on this server.", unknownSocketURI));
        this.unknownSocketURI = unknownSocketURI;
    }
}
