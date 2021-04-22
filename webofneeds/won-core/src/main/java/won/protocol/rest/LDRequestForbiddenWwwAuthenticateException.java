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
        super(resourceUri, message, statusCode);
        this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
    }

    public LDRequestForbiddenWwwAuthenticateException(String message, Throwable cause, URI resourceUri,
                    int statusCode, String wwwAuthenticateHeaderValue) {
        super(resourceUri, message, cause, statusCode);
        this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
    }

    public LDRequestForbiddenWwwAuthenticateException(Throwable cause, URI resourceUri, int statusCode,
                    String wwwAuthenticateHeaderValue) {
        super(resourceUri, cause, statusCode);
        this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
    }

    public LDRequestForbiddenWwwAuthenticateException(String message, Throwable cause, boolean enableSuppression,
                    boolean writableStackTrace, URI resourceUri, int statusCode,
                    String wwwAuthenticateHeaderValue) {
        super(message, resourceUri, cause, enableSuppression, writableStackTrace, statusCode);
        this.wwwAuthenticateHeaderValue = wwwAuthenticateHeaderValue;
    }
}
