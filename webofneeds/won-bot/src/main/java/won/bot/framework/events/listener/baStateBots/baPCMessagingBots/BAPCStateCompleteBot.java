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
 * Time: 14.42
 * To change this template use File | Settings | File Templates.
 */
public class BAPCStateCompleteBot extends BATestBotScript {

    @Override
    protected List<BATestScriptAction> setupActions() {
        List<BATestScriptAction> actions = new ArrayList();
        actions.add(new BATestScriptAction(true, "MESSAGE_COMPLETED", URI.create(WON_BAPC.STATE_ACTIVE.getURI())));
        actions.add(new BATestScriptAction(false, "MESSAGE_CLOSE", URI.create(WON_BAPC.STATE_COMPLETED.getURI())));
        actions.add(new BATestScriptAction(true, "MESSAGE_CLOSED", URI.create(WON_BAPC.STATE_CLOSING.getURI())));
        return actions;
    }
}