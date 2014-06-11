package won.bot.framework.events.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsUriBot;

import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.BATestScriptAction;
import won.bot.framework.events.listener.baStateBots.WON_TX;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Danijel
 * Date: 17.4.14.
 */
public class ActiveCannotCompleteVoteSPUriBot extends BATestBotScript
{

  @Override
  protected List<BATestScriptAction> setupActions() {
    List<BATestScriptAction> actions = new ArrayList();

    actions.add(new BATestScriptAction(false, URI.create(WON_TX.MESSAGE_NOTCOMPLETED.getURI()), URI.create(WON_TX.STATE_NOT_COMPLETING.getURI())));

    return actions;
  }
}