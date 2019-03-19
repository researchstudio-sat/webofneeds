package won.protocol.rest;

import java.net.URI;
import java.text.MessageFormat;

public class LinkedDataFetchingException extends RuntimeException {
    // The URI of the resource that could not be fetched
    private URI resourceUri;

    public LinkedDataFetchingException(URI resourceUri) {
        this(resourceUri, MessageFormat.format("Error fetching linked data for {0}", resourceUri), null);
    }

    public LinkedDataFetchingException(URI resourceUri, String message, Throwable cause) {
        super(message, cause);
        this.resourceUri = resourceUri;
    }

    public LinkedDataFetchingException(URI resourceUri, String message) {
        this(resourceUri, message, null);
    }

    public LinkedDataFetchingException(URI resourceUri, Throwable cause) {
        this(resourceUri, MessageFormat.format("Error fetching linked data for {0}", resourceUri), cause);
    }

    public URI getResourceUri() {
        return resourceUri;
    }

}
