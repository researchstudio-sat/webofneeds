package won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsTextBot;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.BATestScriptAction;
import won.bot.framework.eventbot.listener.baStateBots.NopAction;
import won.node.facet.impl.WON_TX;

/**
 * User: Danijel Date: 30.4.14.
 */
public class CompletedSPClosingBot extends BATestBotScript {
    public CompletedSPClosingBot(final String name) {
        super(name);
    }

    public CompletedSPClosingBot() {
    }

    @Override
    protected List<BATestScriptAction> setupActions() {
        List<BATestScriptAction> actions = new ArrayList();
        actions.add(new NopAction());
        actions.add(new BATestScriptAction(true, "MESSAGE_CLOSE", URI.create(WON_TX.STATE_COMPLETED.getURI())));
        actions.add(new BATestScriptAction(false, "MESSAGE_CLOSED", URI.create(WON_TX.STATE_CLOSING.getURI())));
        return actions;
    }
}