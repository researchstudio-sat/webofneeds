package won.protocol.exception;

import java.net.URI;

public class WrongAddressingInformationException extends WonProtocolException {
    URI[] wrongProperties;
    URI messageUri;

    public WrongAddressingInformationException(String message, URI messageUri, URI... wrongProperties) {
        super(message);
        this.wrongProperties = wrongProperties;
        this.messageUri = messageUri;
    }

    public URI[] getWrongProperties() {
        return wrongProperties;
    }

    public URI getMessageUri() {
        return messageUri;
    }
}
