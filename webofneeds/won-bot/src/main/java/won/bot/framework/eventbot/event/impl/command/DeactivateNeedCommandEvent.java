package won.bot.framework.eventbot.event.impl.command;

import won.bot.framework.eventbot.event.BaseEvent;
import won.bot.framework.eventbot.event.BaseNeedSpecificEvent;
import won.bot.framework.eventbot.event.NeedSpecificEvent;

import java.net.URI;

/**
 * Created by fsuda on 24.02.2017.
 */
public class DeactivateNeedCommandEvent extends BaseNeedSpecificEvent implements NeedSpecificEvent {
    public DeactivateNeedCommandEvent(URI needURI) {
        super(needURI);
    }
}
