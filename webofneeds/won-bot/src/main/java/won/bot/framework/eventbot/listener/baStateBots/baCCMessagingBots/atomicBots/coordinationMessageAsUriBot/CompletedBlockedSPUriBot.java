package won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsUriBot;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.BATestScriptAction;
import won.node.facet.impl.WON_TX;

/**
 * User: Danijel Date: 30.4.14.
 */
public class CompletedBlockedSPUriBot extends BATestBotScript {

    @Override
    protected List<BATestScriptAction> setupActions() {
        List<BATestScriptAction> actions = new ArrayList();

        actions.add(new BATestScriptAction(false, URI.create(WON_TX.MESSAGE_CLOSE.getURI()),
                URI.create(WON_TX.STATE_COMPLETED.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_TX.MESSAGE_CLOSED.getURI()),
                URI.create(WON_TX.STATE_CLOSING.getURI())));
        return actions;
    }
}