package won.bot.framework.eventbot.behaviour;

import won.bot.framework.eventbot.event.BaseEvent;

/**
 * Created by fsuda on 12.05.2017.
 */
public class BotBehaviourEvent extends BaseEvent {
    private BotBehaviour behaviour;

    public BotBehaviourEvent(BotBehaviour behaviour) {
        this.behaviour = behaviour;
    }

    public BotBehaviour getBehaviour() {
        return behaviour;
    }
}
