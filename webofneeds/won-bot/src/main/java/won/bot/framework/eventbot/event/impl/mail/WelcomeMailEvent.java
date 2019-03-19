package won.bot.framework.eventbot.event.impl.mail;

import javax.mail.internet.MimeMessage;

import won.bot.framework.eventbot.event.BaseEvent;

/**
 * Created by hfriedrich on 16.11.2016.
 */
public class WelcomeMailEvent extends BaseEvent {
    private final MimeMessage message;

    public WelcomeMailEvent(MimeMessage message) {
        this.message = message;
    }

    public MimeMessage getMessage() {
        return message;
    }
}
