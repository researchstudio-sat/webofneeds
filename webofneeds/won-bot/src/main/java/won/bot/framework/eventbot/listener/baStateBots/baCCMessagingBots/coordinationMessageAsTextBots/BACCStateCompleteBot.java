package won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsTextBots;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.BATestScriptAction;
import won.node.facet.impl.WON_TX;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 26.2.14.
 * Time: 11.55
 * To change this template use File | Settings | File Templates.
 */

public class BACCStateCompleteBot extends BATestBotScript {

    @Override
    protected List<BATestScriptAction> setupActions() {
        List<BATestScriptAction> actions = new ArrayList();
        actions.add(new BATestScriptAction(false, "MESSAGE_COMPLETE", URI.create(WON_TX.STATE_ACTIVE.getURI())));
        actions.add(new BATestScriptAction(true, "MESSAGE_COMPLETED", URI.create(WON_TX.STATE_COMPLETING.getURI())));
        actions.add(new BATestScriptAction(false, "MESSAGE_CLOSE", URI.create(WON_TX.STATE_COMPLETED.getURI())));
        actions.add(new BATestScriptAction(true, "MESSAGE_CLOSED", URI.create(WON_TX.STATE_CLOSING.getURI())));
        return actions;
    }
}

