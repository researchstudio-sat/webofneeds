package won.bot.framework.eventbot.behaviour;

import java.util.Optional;

import won.bot.framework.eventbot.event.BaseEvent;

/**
 * Created by fsuda on 12.05.2017.
 */
public class BotBehaviourEvent extends BaseEvent {
    private BotBehaviour behaviour;
    private Optional<Object> message;

    public BotBehaviourEvent(BotBehaviour behaviour) {
        this(behaviour, Optional.empty());
    }

    public BotBehaviourEvent(BotBehaviour behaviour, Optional<Object> message) {
        this.behaviour = behaviour;
        this.message = message;
    }

    public BotBehaviour getBehaviour() {
        return behaviour;
    }

    public Optional<Object> getMessage() {
        return message;
    };
}
