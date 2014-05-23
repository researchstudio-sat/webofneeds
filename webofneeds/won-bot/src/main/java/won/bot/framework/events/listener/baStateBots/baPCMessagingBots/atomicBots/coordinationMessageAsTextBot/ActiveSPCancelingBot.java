package won.bot.framework.events.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsTextBot;

import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.BATestScriptAction;
import won.bot.framework.events.listener.baStateBots.NopAction;
import won.bot.framework.events.listener.baStateBots.WON_BA;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Danijel
 * Date: 17.4.14.
 */
public class ActiveSPCancelingBot extends BATestBotScript
{

  @Override
  protected List<BATestScriptAction> setupActions() {
    List<BATestScriptAction> actions = new ArrayList();

    //automatic
    //actions.add(new BATestScriptAction(true, "MESSAGE_CANCEL", URI.create(WON_BA.STATE_ACTIVE.getURI()), 3));

    actions.add(new NopAction());
    actions.add(new BATestScriptAction(true, "MESSAGE_CANCELED", URI.create(WON_BA.STATE_CANCELING.getURI())));

    return actions;
  }
}