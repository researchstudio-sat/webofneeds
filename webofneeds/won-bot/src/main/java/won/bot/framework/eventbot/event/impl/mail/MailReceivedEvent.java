package won.bot.framework.eventbot.event.impl.mail;

import won.bot.framework.eventbot.event.BaseEvent;

import javax.mail.internet.MimeMessage;

/**
 * Created by fsuda on 30.09.2016.
 */
public class MailReceivedEvent extends BaseEvent {
    private final MimeMessage message;

    public MailReceivedEvent(MimeMessage message) {
        this.message = message;
    }

    public MimeMessage getMessage() {
        return message;
    }
}
