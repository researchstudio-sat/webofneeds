package won.bot.framework.events.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsUriBot;

import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.BATestScriptAction;
import won.bot.framework.events.listener.baStateBots.NopAction;
import won.node.facet.impl.WON_TX;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Danijel
 * Date: 17.4.14.
 */
public class CompletedSPCompensatingFailingUriBot extends BATestBotScript
{

  @Override
  protected List<BATestScriptAction> setupActions() {
    List<BATestScriptAction> actions = new ArrayList();

    actions.add(new NopAction());
    actions.add(new BATestScriptAction(true, URI.create(WON_TX.MESSAGE_FAIL.getURI()), URI.create(WON_TX
      .STATE_COMPENSATING.getURI())));
    actions.add(new BATestScriptAction(false, URI.create(WON_TX
      .MESSAGE_FAILED.getURI()), URI.create(WON_TX.STATE_FAILING_COMPENSATING.getURI())));

    return actions;
  }
}