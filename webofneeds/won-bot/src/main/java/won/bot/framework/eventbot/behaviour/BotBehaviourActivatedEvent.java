package won.bot.framework.eventbot.behaviour;

import won.bot.framework.eventbot.event.BaseEvent;

/**
 * Created by fsuda on 12.05.2017.
 */
public class BotBehaviourActivatedEvent extends BotBehaviourEvent {
    public BotBehaviourActivatedEvent(BotBehaviour behaviour) {
        super(behaviour);
    }
}
