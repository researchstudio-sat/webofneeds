package won.protocol.rest;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Optional;

public class LinkedDataFetchingException extends RuntimeException {
    // The URI of the resource that could not be fetched
    private URI resourceUri;
    private Optional<Integer> statusCode = Optional.empty();

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

    public LinkedDataFetchingException(URI resourceUri, int statusCode) {
        this.resourceUri = resourceUri;
        this.statusCode = Optional.of(statusCode);
    }

    public LinkedDataFetchingException(String message, URI resourceUri, int statusCode) {
        super(message);
        this.resourceUri = resourceUri;
        this.statusCode = Optional.of(statusCode);
    }

    public LinkedDataFetchingException(String message, Throwable cause, URI resourceUri,
                    int statusCode) {
        super(message, cause);
        this.resourceUri = resourceUri;
        this.statusCode = Optional.of(statusCode);
    }

    public LinkedDataFetchingException(Throwable cause, URI resourceUri, int statusCode) {
        super(cause);
        this.resourceUri = resourceUri;
        this.statusCode = Optional.of(statusCode);
    }

    public LinkedDataFetchingException(String message, Throwable cause, boolean enableSuppression,
                    boolean writableStackTrace, URI resourceUri, int statusCode) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.resourceUri = resourceUri;
        this.statusCode = Optional.of(statusCode);
    }

    public URI getResourceUri() {
        return resourceUri;
    }

    public Optional<Integer> getStatusCode() {
        return statusCode;
    }
}
