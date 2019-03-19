package won.bot.framework.eventbot.event.impl.mail;

import javax.mail.internet.MimeMessage;

import won.bot.framework.eventbot.event.BaseEvent;

/**
 * Created by fsuda on 18.10.2016.
 */
public class CreateNeedFromMailEvent extends BaseEvent {
    private final MimeMessage message;

    public CreateNeedFromMailEvent(MimeMessage message) {
        this.message = message;
    }

    public MimeMessage getMessage() {
        return message;
    }
}
