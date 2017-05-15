package won.bot.framework.eventbot.behaviour;

/**
 * Created by fsuda on 12.05.2017.
 */
public class BotBehaviourFailedEvent extends BotBehaviourEvent {
    private Exception exception;

    public BotBehaviourFailedEvent(BotBehaviour botBehaviour, Exception exception) {
        super(botBehaviour);
        this.exception = exception;
    }

    public Exception getException() {
        return this.exception;
    }
}
