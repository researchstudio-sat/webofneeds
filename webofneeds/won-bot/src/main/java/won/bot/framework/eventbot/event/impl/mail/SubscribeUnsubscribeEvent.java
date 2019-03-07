package won.bot.framework.eventbot.event.impl.mail;

import javax.mail.internet.MimeMessage;

import won.bot.framework.eventbot.action.impl.mail.model.SubscribeStatus;
import won.bot.framework.eventbot.event.BaseEvent;

/**
 * Created by hfriedrich on 16.11.2016.
 */
public class SubscribeUnsubscribeEvent extends BaseEvent
{
  private final MimeMessage message;
  private final SubscribeStatus subscribeStatus;

  public SubscribeUnsubscribeEvent(MimeMessage message, SubscribeStatus subscribeStatus) {

    this.message = message;
    this.subscribeStatus = subscribeStatus;
  }

  public MimeMessage getMessage() {
    return message;
  }

  public SubscribeStatus getSubscribeStatus() {
    return subscribeStatus;
  }
}
