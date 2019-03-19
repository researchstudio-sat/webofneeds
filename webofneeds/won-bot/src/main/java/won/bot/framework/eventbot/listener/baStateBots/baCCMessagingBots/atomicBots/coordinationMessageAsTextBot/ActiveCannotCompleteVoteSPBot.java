package won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsTextBot;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.BATestScriptAction;
import won.node.facet.impl.WON_TX;

/**
 * User: Danijel Date: 17.4.14.
 */
public class ActiveCannotCompleteVoteSPBot extends BATestBotScript {

    @Override
    protected List<BATestScriptAction> setupActions() {
        List<BATestScriptAction> actions = new ArrayList();

        actions.add(new BATestScriptAction(true, "MESSAGE_CANNOTCOMPLETE", URI.create(WON_TX.STATE_ACTIVE.getURI())));
        // no vote!
        actions.add(new BATestScriptAction(false, "MESSAGE_NOTCOMPLETED",
                URI.create(WON_TX.STATE_NOT_COMPLETING.getURI())));

        return actions;
    }
}