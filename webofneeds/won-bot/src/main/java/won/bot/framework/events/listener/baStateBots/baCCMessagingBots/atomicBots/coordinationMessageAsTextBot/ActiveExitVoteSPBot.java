package won.bot.framework.events.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsTextBot;

import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.BATestScriptAction;
import won.bot.framework.events.listener.baStateBots.WON_BA;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Danijel
 * Date: 17.4.14.
 */
public class ActiveExitVoteSPBot extends BATestBotScript
{

  @Override
  protected List<BATestScriptAction> setupActions() {
    List<BATestScriptAction> actions = new ArrayList();

    actions.add(new BATestScriptAction(true, "MESSAGE_EXIT", URI.create(WON_BA.STATE_ACTIVE.getURI())));
    //no vote!
    actions.add(new BATestScriptAction(false, "MESSAGE_EXITED", URI.create(WON_BA.STATE_EXITING.getURI())));

    return actions;
  }
}