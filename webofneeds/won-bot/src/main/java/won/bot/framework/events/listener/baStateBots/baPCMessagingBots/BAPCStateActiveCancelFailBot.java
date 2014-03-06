package won.bot.framework.events.listener.baStateBots.baPCMessagingBots;

import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.BATestScriptAction;
import won.bot.framework.events.listener.baStateBots.WON_BAPC;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 6.3.14.
 * Time: 16.41
 * To change this template use File | Settings | File Templates.
 */
public class BAPCStateActiveCancelFailBot extends BATestBotScript {

    @Override
    protected List<BATestScriptAction> setupActions() {
        List<BATestScriptAction> actions = new ArrayList();
        actions.add(new BATestScriptAction(false, "MESSAGE_CANCEL", URI.create(WON_BAPC.STATE_ACTIVE.getURI())));
        actions.add(new BATestScriptAction(true, "MESSAGE_FAIL", URI.create(WON_BAPC.STATE_CANCELING.getURI())));
        actions.add(new BATestScriptAction(false, "MESSAGE_FAILED", URI.create(WON_BAPC.STATE_FAILING_ACTIVE_CANCELING.getURI())));
        return actions;
    }
}