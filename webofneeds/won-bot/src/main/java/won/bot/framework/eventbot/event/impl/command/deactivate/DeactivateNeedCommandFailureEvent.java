package won.bot.framework.eventbot.event.impl.command.deactivate;

import won.bot.framework.eventbot.event.BaseNeedSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandFailureEvent;

import java.net.URI;

/**
 * Created by fsuda on 17.05.2017.
 */
public class DeactivateNeedCommandFailureEvent extends BaseNeedSpecificEvent
    implements MessageCommandFailureEvent, DeactivateNeedCommandResultEvent {
  private DeactivateNeedCommandEvent deactivateNeedCommandEvent;
  private String message;

  public DeactivateNeedCommandFailureEvent(URI needURI, DeactivateNeedCommandEvent deactivateNeedCommandEvent,
      String message) {
    super(needURI);
    this.deactivateNeedCommandEvent = deactivateNeedCommandEvent;
    this.message = message;
  }

  public DeactivateNeedCommandFailureEvent(URI needURI, DeactivateNeedCommandEvent deactivateNeedCommandEvent) {
    super(needURI);
    this.deactivateNeedCommandEvent = deactivateNeedCommandEvent;
  }

  @Override public MessageCommandEvent getOriginalCommandEvent() {
    return deactivateNeedCommandEvent;
  }

  @Override public String getMessage() {
    return message;
  }

  @Override public boolean isSuccess() {
    return false;
  }
}
