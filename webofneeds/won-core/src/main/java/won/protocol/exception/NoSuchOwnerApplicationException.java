package won.protocol.exception;

/**
 * User: LEIH-NB Date: 11.11.13
 */
public class NoSuchOwnerApplicationException extends WonProtocolException {
    public NoSuchOwnerApplicationException() {
        super("owner application not found on this server.");
    }
}
