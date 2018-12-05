package won.owner.pojo;

import org.springframework.http.HttpStatus;

/**
 * Created by fsuda on 28.11.2018.
 */
public enum RestStatusResponse {
    USER_CREATED(1200, "User created", HttpStatus.CREATED),
    USER_TRANSFERRED(1201, "User Transfered", HttpStatus.CREATED),
    USER_SIGNED_OUT(1202, "User signed out", HttpStatus.OK),
    USER_ANONYMOUSLINK_SENT(1203, "Anonymous Link sent successfully", HttpStatus.OK),

    USER_NOT_FOUND(1400, "User not found", HttpStatus.NOT_FOUND),
    USER_NOT_SIGNED_IN(1401, "User not signed in", HttpStatus.UNAUTHORIZED),
    USER_BAD_CREDENTIALS(1402, "No such username/password combination registered", HttpStatus.FORBIDDEN),
    USER_NOT_VERIFIED(1403, "E-mail address of the user has not been verified yet", HttpStatus.FORBIDDEN),
    USERNAME_MISMATCH(1404, "User name problem", HttpStatus.BAD_REQUEST),
    USER_ALREADY_EXISTS(1405, "User already exists", HttpStatus.CONFLICT),

    TRANSFERUSER_NOT_FOUND(2400, "Cannot transfer to new user: privateUsername not found", HttpStatus.NOT_FOUND),
    TRANSFERUSER_ALREADY_EXISTS(2401, "Cannot transfer to new user: name is already in use", HttpStatus.CONFLICT),

    TOKEN_VERIFICATION_SUCCESS(3200, "E-Mail verification successful", HttpStatus.OK),
    TOKEN_RESEND_SUCCESS(3201, "E-Mail Verification resent", HttpStatus.OK),
    TOKEN_NOT_FOUND(3400, "Verification Token not found", HttpStatus.NOT_FOUND),
    TOKEN_CREATION_FAILED(3401, "Could not create VerifyToken", HttpStatus.SERVICE_UNAVAILABLE),
    TOKEN_EXPIRED(3403, "Verification Token is expired", HttpStatus.BAD_REQUEST),
    TOKEN_RESEND_FAILED_ALREADY_VERIFIED(3404, "User is already verified, E-Mail will not be sent", HttpStatus.CONFLICT),

    SIGNUP_FAILED(4400, "Registration failed", HttpStatus.BAD_REQUEST),

    SETTINGS_CREATED(5200, "Settings created", HttpStatus.CREATED),

    TOS_ACCEPT_SUCCESS(6200, "Successfully accepted Terms Of Service", HttpStatus.OK),

    EXPORT_SUCCESS(7200, "Successfully started exporting user", HttpStatus.OK),
    EXPORT_IS_ANONYMOUS(7403, "To export with an anonymous user you need to provide an email address", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    RestStatusResponse(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
