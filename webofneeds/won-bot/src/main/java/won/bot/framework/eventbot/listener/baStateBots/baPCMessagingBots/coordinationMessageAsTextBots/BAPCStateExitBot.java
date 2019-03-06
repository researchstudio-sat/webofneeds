package won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.coordinationMessageAsTextBots;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.BATestScriptAction;
import won.node.facet.impl.WON_TX;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 6.3.14.
 * Time: 14.30
 * To change this template use File | Settings | File Templates.
 */
public class BAPCStateExitBot extends BATestBotScript {

    @Override
    protected List<BATestScriptAction> setupActions() {
        List<BATestScriptAction> actions = new ArrayList();
        actions.add(new BATestScriptAction(true, "MESSAGE_EXIT", URI.create(WON_TX.STATE_ACTIVE.getURI())));
        actions.add(new BATestScriptAction(false, "MESSAGE_EXITED", URI.create(WON_TX.STATE_EXITING.getURI())));
        return actions;
    }
}