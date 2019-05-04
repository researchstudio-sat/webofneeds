package won.owner.web.events;

import java.util.Locale;

import org.springframework.context.ApplicationEvent;

import won.owner.model.User;

/**
 * Created by fsuda on 27.11.2018. This Event is used/published as a Result of a
 * successful password change.
 */
public class OnPasswordChangedEvent extends ApplicationEvent {
    private String appUrl;
    private Locale locale;
    private User user;

    public OnPasswordChangedEvent(User user, Locale locale, String appUrl) {
        super(user);
        this.appUrl = appUrl;
        this.locale = locale;
        this.user = user;
    }

    public String getAppUrl() {
        return appUrl;
    }

    public void setAppUrl(String appUrl) {
        this.appUrl = appUrl;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
