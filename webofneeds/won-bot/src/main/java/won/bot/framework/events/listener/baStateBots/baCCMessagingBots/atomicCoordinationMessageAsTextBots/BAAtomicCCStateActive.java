package won.bot.framework.events.listener.baStateBots.baCCMessagingBots.atomicCoordinationMessageAsTextBots;

import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.BATestScriptAction;
import won.bot.framework.events.listener.baStateBots.WON_BA;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Danijel
 * Date: 3.4.14.
 */
public class BAAtomicCCStateActive extends BATestBotScript {

    @Override
    protected List<BATestScriptAction> setupActions() {
        List<BATestScriptAction> actions = new ArrayList();
        actions.add(new BATestScriptAction(true, "MESSAGE_COMPLETED", URI.create(WON_BA.STATE_ACTIVE.getURI())));
        actions.add(new BATestScriptAction(true, "MESSAGE_COMPLETED", URI.create(WON_BA.STATE_ACTIVE.getURI())));
        //automatic: actions.add(new BATestScriptAction(false, "MESSAGE_CANCEL", URI.create(WON_BA.STATE_ACTIVE.getURI()));
        actions.add(new BATestScriptAction(true, "MESSAGE_CANCELED", URI.create(WON_BA.STATE_CANCELING.getURI())));
        return actions;
    }
}