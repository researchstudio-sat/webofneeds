package won.bot.framework.events.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsTextBot;

import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.BATestScriptAction;
import won.bot.framework.events.listener.baStateBots.WON_BA;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Danijel
 * Date: 14.5.14.
 */
public class CompletedSPClosingAdditionalParticipantsBot extends BATestBotScript
{
  public CompletedSPClosingAdditionalParticipantsBot(final String name) {
    super(name);
  }

  public CompletedSPClosingAdditionalParticipantsBot() {
  }

  @Override
  protected List<BATestScriptAction> setupActions() {
    List<BATestScriptAction> actions = new ArrayList();
    actions.add(new BATestScriptAction(true, "MESSAGE_CLOSE", URI.create(WON_BA.STATE_COMPLETED.getURI())));
    actions.add(new BATestScriptAction(false, "MESSAGE_CLOSED", URI.create(WON_BA.STATE_CLOSING.getURI())));
    return actions;
  }
}