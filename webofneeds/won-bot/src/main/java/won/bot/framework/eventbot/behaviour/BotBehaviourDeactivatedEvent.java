package won.bot.framework.eventbot.behaviour;

import java.util.Optional;

/**
 * Created by fsuda on 12.05.2017.
 */
public class BotBehaviourDeactivatedEvent extends BotBehaviourEvent {
  public BotBehaviourDeactivatedEvent(BotBehaviour behaviour) {
    super(behaviour);
  }

  public BotBehaviourDeactivatedEvent(BotBehaviour behaviour, Optional<Object> message) {
    super(behaviour, message);
  }
}
