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

    public static class ForbiddenAuthMethodProvided extends LinkedDataFetchingException {
        private String wwwAuthenticateHeaderValue;

        public ForbiddenAuthMethodProvided(URI resourceUri, String wwwAuthenticateHeaderValue) {
            super(resourceUri);
            this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
        }

        public ForbiddenAuthMethodProvided(URI resourceUri, String message, Throwable cause,
                        String wwwAuthenticateHeaderValue) {
            super(resourceUri, message, cause);
            this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
        }

        public ForbiddenAuthMethodProvided(URI resourceUri, String message, String wwwAuthenticateHeaderValue) {
            super(resourceUri, message);
            this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
        }

        public ForbiddenAuthMethodProvided(URI resourceUri, Throwable cause, String wwwAuthenticateHeaderValue) {
            super(resourceUri, cause);
            this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
        }

        public ForbiddenAuthMethodProvided(URI resourceUri, int statusCode, String wwwAuthenticateHeaderValue) {
            super(resourceUri, statusCode);
            this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
        }
    }

    public static class Forbidden extends LinkedDataFetchingException {
        public Forbidden(URI resourceUri) {
            super(resourceUri);
        }

        public Forbidden(URI resourceUri, String message, Throwable cause) {
            super(resourceUri, message, cause);
        }

        public Forbidden(URI resourceUri, String message) {
            super(resourceUri, message);
        }

        public Forbidden(URI resourceUri, Throwable cause) {
            super(resourceUri, cause);
        }

        public Forbidden(URI resourceUri, int statusCode) {
            super(resourceUri, statusCode);
        }
    }

    public static class Unauthorized extends LinkedDataFetchingException {
        public Unauthorized(URI resourceUri) {
            super(resourceUri);
        }

        public Unauthorized(URI resourceUri, String message, Throwable cause) {
            super(resourceUri, message, cause);
        }

        public Unauthorized(URI resourceUri, String message) {
            super(resourceUri, message);
        }

        public Unauthorized(URI resourceUri, Throwable cause) {
            super(resourceUri, cause);
        }

        public Unauthorized(URI resourceUri, int statusCode) {
            super(resourceUri, statusCode);
        }
    }

    public static class UnauthorizedAuthMethodProvided extends LinkedDataFetchingException {
        private String wwwAuthenticateHeaderValue;

        public UnauthorizedAuthMethodProvided(URI resourceUri, String wwwAuthenticateHeaderValue) {
            super(resourceUri);
            this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
        }

        public UnauthorizedAuthMethodProvided(URI resourceUri, String message, Throwable cause,
                        String wwwAuthenticateHeaderValue) {
            super(resourceUri, message, cause);
            this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
        }

        public UnauthorizedAuthMethodProvided(URI resourceUri, String message, String wwwAuthenticateHeaderValue) {
            super(resourceUri, message);
            this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
        }

        public UnauthorizedAuthMethodProvided(URI resourceUri, Throwable cause, String wwwAuthenticateHeaderValue) {
            super(resourceUri, cause);
            this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
        }

        public UnauthorizedAuthMethodProvided(URI resourceUri, int statusCode, String wwwAuthenticateHeaderValue) {
            super(resourceUri, statusCode);
            this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
        }
    }
}
