package won.protocol.exception;

import java.net.URI;
import java.text.MessageFormat;

/**
 * User: LEIH-NB
 * Date: 20.02.14
 */
public class NoSuchFacetException extends Throwable {
    private URI unknownFacetURI;


    public URI getUnknownNeedURI()
    {
        return unknownFacetURI;
    }

    public NoSuchFacetException(final URI unknownFacetURI)
    {
        super(MessageFormat.format("No need with the URI {0} is known on this server.", unknownFacetURI));
        this.unknownFacetURI = unknownFacetURI;
    }
}
