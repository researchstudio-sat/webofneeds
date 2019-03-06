package won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.coordinationMessageAsUriBots;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.BATestScriptAction;
import won.node.facet.impl.WON_TX;

/**
 * User: Danijel
 * Date: 19.3.14.
 */
public class BAPCStateCompleteWithFailureUriBot extends BATestBotScript {

    @Override
    protected List<BATestScriptAction> setupActions() {
        List<BATestScriptAction> actions = new ArrayList();
        actions.add(new BATestScriptAction(true, URI.create(WON_TX
          .MESSAGE_CANCELED.getURI()), URI.create(WON_TX.STATE_ACTIVE.getURI())));
        actions.add(new BATestScriptAction(false, URI.create(WON_TX
          .MESSAGE_EXITED.getURI()), URI.create(WON_TX.STATE_ACTIVE.getURI())));
        //actions.add(new BATestScriptAction(true, URI.create(WON_BA.MESSAGE_EXITED.getURI()), URI.create(WON_BA.STATE_ACTIVE.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_TX
          .MESSAGE_COMPLETED.getURI()), URI.create(WON_TX.STATE_ACTIVE.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_TX
          .MESSAGE_COMPLETED.getURI()), URI.create(WON_TX.STATE_ACTIVE.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_TX
          .MESSAGE_EXIT.getURI()), URI.create(WON_TX.STATE_COMPLETED.getURI())));
        actions.add(new BATestScriptAction(false, URI.create(WON_TX
          .MESSAGE_CANCEL.getURI()), URI.create(WON_TX.STATE_COMPLETED.getURI())));
        actions.add(new BATestScriptAction(false, URI.create(WON_TX
          .MESSAGE_CLOSE.getURI()), URI.create(WON_TX.STATE_COMPLETED.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_TX
          .MESSAGE_CANNOTCOMPLETE.getURI()), URI.create(WON_TX.STATE_COMPLETED.getURI())));
        actions.add(new BATestScriptAction(false, URI.create(WON_TX
          .MESSAGE_CANNOTCOMPLETE.getURI()), URI.create(WON_TX.STATE_COMPLETED.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_TX
          .MESSAGE_CLOSED.getURI()), URI.create(WON_TX.STATE_CLOSING.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_TX
          .MESSAGE_CLOSED.getURI()), URI.create(WON_TX.STATE_ENDED.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_TX
          .MESSAGE_EXIT.getURI()), URI.create(WON_TX.STATE_ENDED.getURI())));
        actions.add(new BATestScriptAction(false, URI.create(WON_TX
          .MESSAGE_EXIT.getURI()), URI.create(WON_TX.STATE_ENDED.getURI())));
        return actions;
    }
}