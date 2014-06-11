package won.bot.framework.events.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsTextBot;

import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.BATestScriptAction;
import won.bot.framework.events.listener.baStateBots.WON_TX;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Danijel
 * Date: 24.4.14.
 */
public class CompletingExitVoteSPBot extends BATestBotScript
{

  @Override
  protected List<BATestScriptAction> setupActions() {
    List<BATestScriptAction> actions = new ArrayList();

    actions.add(new BATestScriptAction(true, "MESSAGE_EXIT", URI.create(WON_TX.STATE_COMPLETING.getURI())));
    //no vote!
    actions.add(new BATestScriptAction(false, "MESSAGE_EXITED", URI.create(WON_TX.STATE_EXITING.getURI())));

    return actions;
  }
}