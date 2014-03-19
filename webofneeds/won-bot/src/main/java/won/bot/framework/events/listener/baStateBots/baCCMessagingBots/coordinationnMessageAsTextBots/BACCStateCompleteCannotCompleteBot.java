package won.bot.framework.events.listener.baStateBots.baCCMessagingBots.coordinationnMessageAsTextBots;

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
 * Time: 13.30
 * To change this template use File | Settings | File Templates.
 */
public class BACCStateCompleteCannotCompleteBot extends BATestBotScript {

    @Override
    protected List<BATestScriptAction> setupActions() {
        List<BATestScriptAction> actions = new ArrayList();
        actions.add(new BATestScriptAction(false, "MESSAGE_COMPLETE", URI.create(WON_BA.STATE_ACTIVE.getURI())));
        actions.add(new BATestScriptAction(true, "MESSAGE_CANNOTCOMPLETE", URI.create(WON_BA.STATE_COMPLETING.getURI())));
        actions.add(new BATestScriptAction(false, "MESSAGE_NOTCOMPLETED", URI.create(WON_BA.STATE_NOT_COMPLETING.getURI())));
        return actions;
    }
}