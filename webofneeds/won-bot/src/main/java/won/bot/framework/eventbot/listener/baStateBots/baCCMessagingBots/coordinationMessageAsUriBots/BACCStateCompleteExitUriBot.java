package won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsUriBots;

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
 * Time: 13.06
 * To change this template use File | Settings | File Templates.
 */
public class BACCStateCompleteExitUriBot extends BATestBotScript {

    @Override
    protected List<BATestScriptAction> setupActions() {
        List<BATestScriptAction> actions = new ArrayList();
        actions.add(new BATestScriptAction(false, URI.create(WON_TX
          .MESSAGE_COMPLETE.getURI()), URI.create(WON_TX.STATE_ACTIVE.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_TX
          .MESSAGE_EXIT.getURI()), URI.create(WON_TX.STATE_COMPLETING.getURI())));
        actions.add(new BATestScriptAction(false, URI.create(WON_TX
          .MESSAGE_EXITED.getURI()), URI.create(WON_TX.STATE_EXITING.getURI())));
        return actions;
    }
}
