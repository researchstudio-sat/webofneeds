package won.owner.web.events;

import org.springframework.context.ApplicationEvent;
import org.springframework.security.core.Authentication;

public class OnExportUserEvent extends ApplicationEvent {
    private final Authentication authentication;
    private final String keyStorePassword;
    private final String responseEmail;

    public OnExportUserEvent(Authentication authentication, String keyStorePassword, String responseEmail) {
        super(authentication);
        this.authentication = authentication;
        this.keyStorePassword = keyStorePassword;
        this.responseEmail = responseEmail;
    }

    public String getResponseEmail() {
        return responseEmail;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }
}
