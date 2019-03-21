package won.bot.framework.eventbot.behaviour;

import java.util.Optional;

/**
 * Created by fsuda on 12.05.2017.
 */
public class BotBehaviourFailedEvent extends BotBehaviourEvent {
  private Exception exception;

  public BotBehaviourFailedEvent(BotBehaviour botBehaviour, Exception exception) {
    super(botBehaviour);
    this.exception = exception;
  }

  public BotBehaviourFailedEvent(BotBehaviour behaviour, Optional<Object> message, Exception exception) {
    super(behaviour, message);
    this.exception = exception;
  }

  public Exception getException() {
    return this.exception;
  }
}
