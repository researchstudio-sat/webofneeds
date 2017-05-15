package won.bot.framework.eventbot.behaviour;

import won.bot.framework.eventbot.event.BaseEvent;

/**
 * Created by fsuda on 12.05.2017.
 */
public class BotBehaviourDeactivatedEvent extends BotBehaviourEvent {
    public BotBehaviourDeactivatedEvent(BotBehaviour behaviour) {
        super(behaviour);
    }
}
