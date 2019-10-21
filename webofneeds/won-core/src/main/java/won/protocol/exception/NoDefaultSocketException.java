package won.protocol.exception;

import java.net.URI;
import java.text.MessageFormat;

public class NoDefaultSocketException extends RuntimeException {
    private static final long serialVersionUID = -8417204478048969672L;
    private URI atomUri;

    public URI getAtomURI() {
        return atomUri;
    }

    public NoDefaultSocketException(final URI atomUri) {
        super(MessageFormat.format("Cannot determine default socket of atom {0}", atomUri));
        this.atomUri = atomUri;
    }
}
