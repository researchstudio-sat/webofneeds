package won.bot.framework.events.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsUriBot;

import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.BATestScriptAction;
import won.bot.framework.events.listener.baStateBots.NopAction;
import won.bot.framework.events.listener.baStateBots.WON_TX;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Danijel
 * Date: 17.4.14.
 */
public class ActiveSPCancelingUriBot extends BATestBotScript
{

  @Override
  protected List<BATestScriptAction> setupActions() {
    List<BATestScriptAction> actions = new ArrayList();


    actions.add(new NopAction());
    actions.add(new BATestScriptAction(true, URI.create(WON_TX.MESSAGE_CANCELED.getURI()), URI.create(WON_TX.STATE_CANCELING.getURI())));

    return actions;
  }
}