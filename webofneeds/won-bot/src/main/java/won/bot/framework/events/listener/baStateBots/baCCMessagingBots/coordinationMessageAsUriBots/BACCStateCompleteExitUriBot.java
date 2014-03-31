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
 * Date: 6.3.14.
 * Time: 13.06
 * To change this template use File | Settings | File Templates.
 */
public class BACCStateCompleteExitUriBot extends BATestBotScript {

    @Override
    protected List<BATestScriptAction> setupActions() {
        List<BATestScriptAction> actions = new ArrayList();
        actions.add(new BATestScriptAction(false, URI.create(WON_BA.MESSAGE_COMPLETE.getURI()), URI.create(WON_BA.STATE_ACTIVE.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_BA.MESSAGE_EXIT.getURI()), URI.create(WON_BA.STATE_COMPLETING.getURI())));
        actions.add(new BATestScriptAction(false, URI.create(WON_BA.MESSAGE_EXITED.getURI()), URI.create(WON_BA.STATE_EXITING.getURI())));
        return actions;
    }
}
