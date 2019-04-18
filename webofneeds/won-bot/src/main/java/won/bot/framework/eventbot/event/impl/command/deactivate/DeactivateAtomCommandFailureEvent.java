package won.bot.framework.eventbot.event.impl.command.deactivate;

import java.net.URI;

import won.bot.framework.eventbot.event.BaseAtomSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandFailureEvent;

/**
 * Created by fsuda on 17.05.2017.
 */
public class DeactivateAtomCommandFailureEvent extends BaseAtomSpecificEvent
                implements MessageCommandFailureEvent, DeactivateAtomCommandResultEvent {
    private DeactivateAtomCommandEvent deactivateAtomCommandEvent;
    private String message;

    public DeactivateAtomCommandFailureEvent(URI atomURI, DeactivateAtomCommandEvent deactivateAtomCommandEvent,
                    String message) {
        super(atomURI);
        this.deactivateAtomCommandEvent = deactivateAtomCommandEvent;
        this.message = message;
    }

    public DeactivateAtomCommandFailureEvent(URI atomURI, DeactivateAtomCommandEvent deactivateAtomCommandEvent) {
        super(atomURI);
        this.deactivateAtomCommandEvent = deactivateAtomCommandEvent;
    }

    @Override
    public MessageCommandEvent getOriginalCommandEvent() {
        return deactivateAtomCommandEvent;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }
}
