package won.bot.framework.events.event.impl.debugbot;

import won.protocol.model.Connection;

/**
 * User: ypanchenko
 * Date: 26.02.2016
 */
public class DeactivateDebugCommandEvent extends DebugCommandEvent
{

  public DeactivateDebugCommandEvent(final Connection con) {
    super(con);
  }
}
