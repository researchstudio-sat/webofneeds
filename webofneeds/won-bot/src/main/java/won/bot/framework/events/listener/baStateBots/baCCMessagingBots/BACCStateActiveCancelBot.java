package won.bot.framework.events.listener.baStateBots.baCCMessagingBots;

import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.BATestScriptAction;
import won.bot.framework.events.listener.baStateBots.WON_BACC;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 6.3.14.
 * Time: 12.42
 * To change this template use File | Settings | File Templates.
 */
public class BACCStateActiveCancelBot extends BATestBotScript {

    @Override
    protected List<BATestScriptAction> setupActions() {
        List<BATestScriptAction> actions = new ArrayList();
        actions.add(new BATestScriptAction(false, "MESSAGE_CANCEL", URI.create(WON_BACC.STATE_ACTIVE.getURI())));
        actions.add(new BATestScriptAction(true, "MESSAGE_CANCELED", URI.create(WON_BACC.STATE_CANCELING_ACTIVE.getURI())));
        return actions;
    }
}
