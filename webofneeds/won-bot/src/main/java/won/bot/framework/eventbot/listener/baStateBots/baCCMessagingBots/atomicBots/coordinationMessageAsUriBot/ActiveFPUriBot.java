package won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsUriBot;

import java.util.ArrayList;
import java.util.List;

import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.BATestScriptAction;
import won.bot.framework.eventbot.listener.baStateBots.NopAction;

/**
 * User: Danijel
 * Date: 17.4.14.
 */
public class ActiveFPUriBot extends BATestBotScript
{

  @Override
  protected List<BATestScriptAction> setupActions() {
    List<BATestScriptAction> actions = new ArrayList();

    actions.add(new NopAction());

    return actions;
  }
}
