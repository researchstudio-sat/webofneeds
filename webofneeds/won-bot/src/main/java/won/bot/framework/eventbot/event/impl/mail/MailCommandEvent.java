package won.bot.framework.eventbot.event.impl.mail;

import won.bot.framework.eventbot.event.BaseEvent;

import javax.mail.internet.MimeMessage;

/**
 * Created by fsuda on 18.10.2016.
 */
public class MailCommandEvent extends BaseEvent {
  private final MimeMessage message;

  public MailCommandEvent(MimeMessage message) {
    this.message = message;
  }

  public MimeMessage getMessage() {
    return message;
  }
}