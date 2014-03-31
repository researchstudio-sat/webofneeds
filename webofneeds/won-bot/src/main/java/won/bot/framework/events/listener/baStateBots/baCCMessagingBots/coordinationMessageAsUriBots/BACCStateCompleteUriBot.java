package won.bot.framework.events.listener.baStateBots.baCCMessagingBots.coordinationMessageAsUriBots;

import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.BATestScriptAction;
import won.bot.framework.events.listener.baStateBots.WON_BA;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 26.2.14.
 * Time: 11.55
 * To change this template use File | Settings | File Templates.
 */

public class BACCStateCompleteUriBot extends BATestBotScript {

    @Override
    protected List<BATestScriptAction> setupActions() {
        List<BATestScriptAction> actions = new ArrayList();
        actions.add(new BATestScriptAction(false, URI.create(WON_BA.MESSAGE_COMPLETE.getURI()), URI.create(WON_BA.STATE_ACTIVE.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_BA.MESSAGE_COMPLETED.getURI()), URI.create(WON_BA.STATE_COMPLETING.getURI())));
        actions.add(new BATestScriptAction(false, URI.create(WON_BA.MESSAGE_CLOSE.getURI()), URI.create(WON_BA.STATE_COMPLETED.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_BA.MESSAGE_CLOSED.getURI()), URI.create(WON_BA.STATE_CLOSING.getURI())));
        return actions;
    }
}

