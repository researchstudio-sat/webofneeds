package won.protocol.exception;

import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Property;

public class WrongAddressingInformationException extends WonProtocolException {
    URI[] wrongProperties;
    URI messageUri;

    public WrongAddressingInformationException(String message, URI messageUri, URI... wrongProperties) {
        super(message);
        this.wrongProperties = wrongProperties;
        this.messageUri = messageUri;
    }

    public WrongAddressingInformationException(String message, URI messageUri, Property... wrongProperties) {
        super(message);
        this.wrongProperties = Arrays.stream(wrongProperties).map(p -> URI.create(p.getURI()))
                        .collect(Collectors.toList()).toArray(new URI[] {});
        this.messageUri = messageUri;
    }

    public URI[] getWrongProperties() {
        return wrongProperties;
    }

    public URI getMessageUri() {
        return messageUri;
    }
}
