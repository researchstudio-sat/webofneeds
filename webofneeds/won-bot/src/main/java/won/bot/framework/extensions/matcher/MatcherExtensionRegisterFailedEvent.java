package won.bot.framework.extensions.matcher;

import java.net.URI;

import won.bot.framework.eventbot.event.BaseNodeSpecificEvent;

/**
 * Created by hfriedrich on 26.01.2017.
 */
public class MatcherExtensionRegisterFailedEvent extends BaseNodeSpecificEvent {
    public MatcherExtensionRegisterFailedEvent(final URI wonNodeURI) {
        super(wonNodeURI);
    }
}
