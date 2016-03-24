package won.bot.framework.events.event.impl.debugbot;

import won.bot.framework.events.event.BaseNeedAndConnectionSpecificEvent;
import won.protocol.model.Connection;

/**
 * User: ypanchenko
 * Date: 26.02.2016
 */
public abstract class DebugCommandEvent extends BaseNeedAndConnectionSpecificEvent
{

  public DebugCommandEvent(final Connection con) {
    super(con);
  }

}
