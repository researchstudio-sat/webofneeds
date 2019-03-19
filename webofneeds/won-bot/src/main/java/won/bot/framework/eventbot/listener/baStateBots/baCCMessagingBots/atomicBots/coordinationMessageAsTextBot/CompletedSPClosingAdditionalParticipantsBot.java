package won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsTextBot;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.BATestScriptAction;
import won.node.facet.impl.WON_TX;

/**
 * User: Danijel Date: 14.5.14.
 */
public class CompletedSPClosingAdditionalParticipantsBot extends BATestBotScript {
    public CompletedSPClosingAdditionalParticipantsBot(final String name) {
        super(name);
    }

    public CompletedSPClosingAdditionalParticipantsBot() {
    }

    @Override
    protected List<BATestScriptAction> setupActions() {
        List<BATestScriptAction> actions = new ArrayList();
        actions.add(new BATestScriptAction(true, "MESSAGE_CLOSE", URI.create(WON_TX.STATE_COMPLETED.getURI())));
        actions.add(new BATestScriptAction(false, "MESSAGE_CLOSED", URI.create(WON_TX.STATE_CLOSING.getURI())));
        return actions;
    }
}