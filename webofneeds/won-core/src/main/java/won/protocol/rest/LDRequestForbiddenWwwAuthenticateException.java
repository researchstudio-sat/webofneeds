package won.protocol.rest;

import java.net.URI;

public class LDRequestForbiddenWwwAuthenticateException extends LinkedDataFetchingException {
    private String wwwAuthenticateHeaderValue;

    public LDRequestForbiddenWwwAuthenticateException(URI resourceUri, String wwwAuthenticateHeaderValue) {
        super(resourceUri);
        this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
    }

    public LDRequestForbiddenWwwAuthenticateException(URI resourceUri, String message, Throwable cause,
                    String wwwAuthenticateHeaderValue) {
        super(resourceUri, message, cause);
        this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
    }

    public LDRequestForbiddenWwwAuthenticateException(URI resourceUri, String message,
                    String wwwAuthenticateHeaderValue) {
        super(resourceUri, message);
        this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
    }

    public LDRequestForbiddenWwwAuthenticateException(URI resourceUri, Throwable cause,
                    String wwwAuthenticateHeaderValue) {
        super(resourceUri, cause);
        this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
    }

    public LDRequestForbiddenWwwAuthenticateException(URI resourceUri, int statusCode,
                    String wwwAuthenticateHeaderValue) {
        super(resourceUri, statusCode);
        this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
    }

    public LDRequestForbiddenWwwAuthenticateException(String message, URI resourceUri, int statusCode,
                    String wwwAuthenticateHeaderValue) {
        super(message, resourceUri, statusCode);
        this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
    }

    public LDRequestForbiddenWwwAuthenticateException(String message, Throwable cause, URI resourceUri,
                    int statusCode, String wwwAuthenticateHeaderValue) {
        super(message, cause, resourceUri, statusCode);
        this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
    }

    public LDRequestForbiddenWwwAuthenticateException(Throwable cause, URI resourceUri, int statusCode,
                    String wwwAuthenticateHeaderValue) {
        super(cause, resourceUri, statusCode);
        this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
    }

    public LDRequestForbiddenWwwAuthenticateException(String message, Throwable cause, boolean enableSuppression,
                    boolean writableStackTrace, URI resourceUri, int statusCode,
                    String wwwAuthenticateHeaderValue) {
        super(message, cause, enableSuppression, writableStackTrace, resourceUri, statusCode);
        this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
    }
}
