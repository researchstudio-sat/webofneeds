package won.protocol.rest;

import org.springframework.http.HttpStatus;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * Exception thrown by methods trying to access a specific linked data resource.
 * The resource is always present in the exception, optionally, the http status
 * code is provided as well.
 * <p>
 * Some Subclasses are available for specific cases such as Forbidden and
 * Unauthorized.
 * </p>
 */
public class LinkedDataFetchingException extends RuntimeException {
    // The URI of the resource that could not be fetched
    private URI resourceUri;
    private Integer statusCode = null;

    public LinkedDataFetchingException(URI resourceUri) {
        this(resourceUri, MessageFormat.format("Error fetching linked data for {0}", resourceUri), null);
    }

    public LinkedDataFetchingException(URI resourceUri, String message, Throwable cause) {
        super(message, cause);
        Objects.requireNonNull(resourceUri);
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
        this.statusCode = statusCode;
        Objects.requireNonNull(resourceUri);
    }

    public LinkedDataFetchingException(URI resourceUri, String message, int statusCode) {
        super(message);
        Objects.requireNonNull(resourceUri);
        this.resourceUri = resourceUri;
        this.statusCode = statusCode;
    }

    public LinkedDataFetchingException(URI resourceUri, String message, Throwable cause,
                    int statusCode) {
        super(message, cause);
        Objects.requireNonNull(resourceUri);
        this.resourceUri = resourceUri;
        this.statusCode = statusCode;
    }

    public LinkedDataFetchingException(URI resourceUri, Throwable cause, int statusCode) {
        super(cause);
        Objects.requireNonNull(resourceUri);
        this.resourceUri = resourceUri;
        this.statusCode = statusCode;
    }

    public LinkedDataFetchingException(String message, URI resourceUri, Throwable cause, boolean enableSuppression,
                    boolean writableStackTrace, int statusCode) {
        super(message, cause, enableSuppression, writableStackTrace);
        Objects.requireNonNull(resourceUri);
        this.resourceUri = resourceUri;
        this.statusCode = statusCode;
    }

    public URI getResourceUri() {
        return resourceUri;
    }

    public Optional<Integer> getStatusCode() {
        return Optional.ofNullable(statusCode);
    }

    public static class ForbiddenAuthMethodProvided extends LinkedDataFetchingException {
        private String wwwAuthenticateHeaderValue;

        public ForbiddenAuthMethodProvided(URI resourceUri, String wwwAuthenticateHeaderValue) {
            super(resourceUri, HttpStatus.FORBIDDEN.value());
            Objects.requireNonNull(wwwAuthenticateHeaderValue);
            this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
        }

        public ForbiddenAuthMethodProvided(URI resourceUri, String message, Throwable cause,
                        String wwwAuthenticateHeaderValue) {
            super(resourceUri, message, cause, HttpStatus.FORBIDDEN.value());
            Objects.requireNonNull(wwwAuthenticateHeaderValue);
            this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
        }

        public ForbiddenAuthMethodProvided(URI resourceUri, String message, String wwwAuthenticateHeaderValue) {
            super(resourceUri, message, HttpStatus.FORBIDDEN.value());
            Objects.requireNonNull(wwwAuthenticateHeaderValue);
            this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
        }

        public ForbiddenAuthMethodProvided(URI resourceUri, Throwable cause, String wwwAuthenticateHeaderValue) {
            super(resourceUri, cause, HttpStatus.FORBIDDEN.value());
            Objects.requireNonNull(wwwAuthenticateHeaderValue);
            this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
        }
    }

    public static class Forbidden extends LinkedDataFetchingException {
        public Forbidden(URI resourceUri) {
            super(resourceUri, HttpStatus.FORBIDDEN.value());
        }

        public Forbidden(URI resourceUri, String message, Throwable cause) {
            super(resourceUri, message, cause, HttpStatus.FORBIDDEN.value());
        }

        public Forbidden(URI resourceUri, String message) {
            super(resourceUri, message, HttpStatus.FORBIDDEN.value());
        }

        public Forbidden(URI resourceUri, Throwable cause) {
            super(resourceUri, cause, HttpStatus.FORBIDDEN.value());
        }
    }

    public static class Unauthorized extends LinkedDataFetchingException {
        public Unauthorized(URI resourceUri) {
            super(resourceUri, HttpStatus.UNAUTHORIZED.value());
        }

        public Unauthorized(URI resourceUri, String message, Throwable cause) {
            super(resourceUri, message, cause, HttpStatus.UNAUTHORIZED.value());
        }

        public Unauthorized(URI resourceUri, String message) {
            super(resourceUri, message, HttpStatus.UNAUTHORIZED.value());
        }

        public Unauthorized(URI resourceUri, Throwable cause) {
            super(resourceUri, cause, HttpStatus.UNAUTHORIZED.value());
        }

        public Unauthorized(URI resourceUri, int statusCode) {
            super(resourceUri, statusCode);
        }
    }

    public static class UnauthorizedAuthMethodProvided extends LinkedDataFetchingException {
        private String wwwAuthenticateHeaderValue;

        public UnauthorizedAuthMethodProvided(URI resourceUri, String wwwAuthenticateHeaderValue) {
            super(resourceUri, HttpStatus.UNAUTHORIZED.value());
            Objects.requireNonNull(wwwAuthenticateHeaderValue);
            this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
        }

        public UnauthorizedAuthMethodProvided(URI resourceUri, String message, Throwable cause,
                        String wwwAuthenticateHeaderValue) {
            super(resourceUri, message, cause, HttpStatus.UNAUTHORIZED.value());
            Objects.requireNonNull(wwwAuthenticateHeaderValue);
            this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
        }

        public UnauthorizedAuthMethodProvided(URI resourceUri, String message, String wwwAuthenticateHeaderValue) {
            super(resourceUri, message, HttpStatus.UNAUTHORIZED.value());
            Objects.requireNonNull(wwwAuthenticateHeaderValue);
            this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
        }

        public UnauthorizedAuthMethodProvided(URI resourceUri, Throwable cause, String wwwAuthenticateHeaderValue) {
            super(resourceUri, cause, HttpStatus.UNAUTHORIZED.value());
            Objects.requireNonNull(wwwAuthenticateHeaderValue);
            this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
        }
    }
}
