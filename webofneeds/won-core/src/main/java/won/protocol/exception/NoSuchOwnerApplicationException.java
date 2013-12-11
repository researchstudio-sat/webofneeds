package won.protocol.exception;

import java.net.URI;
import java.text.MessageFormat;

/**
 * User: LEIH-NB
 * Date: 11.11.13
 */
public class NoSuchOwnerApplicationException extends WonProtocolException {
    private URI unknownNeedURI;


    public URI getUnknownNeedURI()
    {
        return unknownNeedURI;
    }

    public NoSuchOwnerApplicationException()
    {
        super(MessageFormat.format("owner application not found on this server.", null));

    }
}
