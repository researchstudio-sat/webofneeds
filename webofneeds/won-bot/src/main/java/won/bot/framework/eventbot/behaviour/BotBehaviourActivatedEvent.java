package won.bot.framework.eventbot.behaviour;

import java.util.Optional;

/**
 * Created by fsuda on 12.05.2017.
 */
public class BotBehaviourActivatedEvent extends BotBehaviourEvent {
  public BotBehaviourActivatedEvent(BotBehaviour behaviour) {
    super(behaviour);
  }

  public BotBehaviourActivatedEvent(BotBehaviour behaviour, Optional<Object> message) {
    super(behaviour, message);
  }
}
