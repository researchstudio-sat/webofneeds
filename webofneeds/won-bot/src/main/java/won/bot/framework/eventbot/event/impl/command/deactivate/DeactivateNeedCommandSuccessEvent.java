package won.bot.framework.eventbot.event.impl.command.deactivate;

import java.net.URI;

import won.bot.framework.eventbot.event.BaseNeedSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandSuccessEvent;

/**
 * Indicates that deactivating a need succeeded.
 */
public class DeactivateNeedCommandSuccessEvent extends BaseNeedSpecificEvent
                implements MessageCommandSuccessEvent, DeactivateNeedCommandResultEvent {
    private DeactivateNeedCommandEvent deactivateNeedCommandEvent;
    private String message;

    public DeactivateNeedCommandSuccessEvent(URI needURI, DeactivateNeedCommandEvent deactivateNeedCommandEvent,
                    String message) {
        super(needURI);
        this.deactivateNeedCommandEvent = deactivateNeedCommandEvent;
        this.message = message;
    }

    public DeactivateNeedCommandSuccessEvent(URI needURI, DeactivateNeedCommandEvent deactivateNeedCommandEvent) {
        super(needURI);
        this.deactivateNeedCommandEvent = deactivateNeedCommandEvent;
    }

    @Override
    public MessageCommandEvent getOriginalCommandEvent() {
        return deactivateNeedCommandEvent;
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
