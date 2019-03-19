package won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsTextBots;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.BATestScriptAction;
import won.node.facet.impl.WON_TX;

/**
 * Created with IntelliJ IDEA. User: Danijel Date: 6.3.14. Time: 12.42 To change this template use File | Settings |
 * File Templates.
 */
public class BACCStateActiveCancelBot extends BATestBotScript {

    @Override
    protected List<BATestScriptAction> setupActions() {
        List<BATestScriptAction> actions = new ArrayList();
        actions.add(new BATestScriptAction(false, "MESSAGE_CANCEL", URI.create(WON_TX.STATE_ACTIVE.getURI())));
        actions.add(
                new BATestScriptAction(true, "MESSAGE_CANCELED", URI.create(WON_TX.STATE_CANCELING_ACTIVE.getURI())));
        return actions;
    }
}
