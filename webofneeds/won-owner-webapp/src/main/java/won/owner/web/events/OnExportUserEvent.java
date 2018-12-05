package won.owner.web.events;

import org.springframework.context.ApplicationEvent;
import org.springframework.security.core.Authentication;
import won.owner.model.User;

public class OnExportUserEvent extends ApplicationEvent {
    private final Authentication authentication;
    private final String responseEmail;
    private final User user;

    public OnExportUserEvent(Authentication authentication, User user, String responseEmail) {
        super(authentication);
        this.authentication = authentication;
        this.user = user;
        this.responseEmail = responseEmail;
    }

    public String getResponseEmail() {
        return responseEmail;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public User getUser() {
        return user;
    }
}
