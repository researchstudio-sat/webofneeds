package won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsUriBot;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.BATestScriptAction;
import won.bot.framework.eventbot.listener.baStateBots.NopAction;
import won.node.facet.impl.WON_TX;

/**
 * User: Danijel
 * Date: 17.4.14.
 */
public class CompletingSPCancelingUriBot extends BATestBotScript
{

  @Override
  protected List<BATestScriptAction> setupActions() {
    List<BATestScriptAction> actions = new ArrayList();

    //automatic
    //actions.add(new BATestScriptAction(false, "MESSAGE_CANCEL", URI.create(WON_BA.STATE_COMPLETING.getURI()));

    actions.add(new NopAction());
    actions.add(new BATestScriptAction(true, URI.create(WON_TX
      .MESSAGE_CANCELED.getURI()), URI.create(WON_TX.STATE_CANCELING_COMPLETING.getURI())));

    return actions;
  }
}