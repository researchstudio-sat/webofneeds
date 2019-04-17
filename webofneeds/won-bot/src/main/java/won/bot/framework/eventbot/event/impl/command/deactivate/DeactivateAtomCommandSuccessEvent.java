package won.bot.framework.eventbot.event.impl.command.deactivate;

import java.net.URI;

import won.bot.framework.eventbot.event.BaseAtomSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandSuccessEvent;

/**
 * Indicates that deactivating an atom succeeded.
 */
public class DeactivateAtomCommandSuccessEvent extends BaseAtomSpecificEvent
                implements MessageCommandSuccessEvent, DeactivateAtomCommandResultEvent {
    private DeactivateAtomCommandEvent deactivateAtomCommandEvent;
    private String message;

    public DeactivateAtomCommandSuccessEvent(URI atomURI, DeactivateAtomCommandEvent deactivateAtomCommandEvent,
                    String message) {
        super(atomURI);
        this.deactivateAtomCommandEvent = deactivateAtomCommandEvent;
        this.message = message;
    }

    public DeactivateAtomCommandSuccessEvent(URI atomURI, DeactivateAtomCommandEvent deactivateAtomCommandEvent) {
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
        return true;
    }
}
