package won.bot.framework.eventbot.event.impl.matcher;

import won.bot.framework.eventbot.event.BaseNodeSpecificEvent;

import java.net.URI;

/**
 * Created by hfriedrich on 26.01.2017.
 */
public class MatcherRegisterFailedEvent extends BaseNodeSpecificEvent {
  public MatcherRegisterFailedEvent(final URI wonNodeURI) {
    super(wonNodeURI);
  }
}
