package won.bot.framework.events.listener.baStateBots.baPCMessagingBots.coordinationMessageAsUriBots;

import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.BATestScriptAction;
import won.bot.framework.events.listener.baStateBots.WON_BA;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Danijel
 * Date: 19.3.14.
 */
public class BAPCStateCompleteWithFailureUriBot extends BATestBotScript {

    @Override
    protected List<BATestScriptAction> setupActions() {
        List<BATestScriptAction> actions = new ArrayList();
        actions.add(new BATestScriptAction(true, URI.create(WON_BA.MESSAGE_CANCELED.getURI()), URI.create(WON_BA.STATE_ACTIVE.getURI())));
        actions.add(new BATestScriptAction(false, URI.create(WON_BA.MESSAGE_EXITED.getURI()), URI.create(WON_BA.STATE_ACTIVE.getURI())));
        //actions.add(new BATestScriptAction(true, URI.create(WON_BA.MESSAGE_EXITED.getURI()), URI.create(WON_BA.STATE_ACTIVE.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_BA.MESSAGE_COMPLETED.getURI()), URI.create(WON_BA.STATE_ACTIVE.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_BA.MESSAGE_COMPLETED.getURI()), URI.create(WON_BA.STATE_ACTIVE.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_BA.MESSAGE_EXIT.getURI()), URI.create(WON_BA.STATE_COMPLETED.getURI())));
        actions.add(new BATestScriptAction(false, URI.create(WON_BA.MESSAGE_CANCEL.getURI()), URI.create(WON_BA.STATE_COMPLETED.getURI())));
        actions.add(new BATestScriptAction(false, URI.create(WON_BA.MESSAGE_CLOSE.getURI()), URI.create(WON_BA.STATE_COMPLETED.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_BA.MESSAGE_CANNOTCOMPLETE.getURI()), URI.create(WON_BA.STATE_COMPLETED.getURI())));
        actions.add(new BATestScriptAction(false, URI.create(WON_BA.MESSAGE_CANNOTCOMPLETE.getURI()), URI.create(WON_BA.STATE_COMPLETED.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_BA.MESSAGE_CLOSED.getURI()), URI.create(WON_BA.STATE_CLOSING.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_BA.MESSAGE_CLOSED.getURI()), URI.create(WON_BA.STATE_ENDED.getURI())));
        actions.add(new BATestScriptAction(true, URI.create(WON_BA.MESSAGE_EXIT.getURI()), URI.create(WON_BA.STATE_ENDED.getURI())));
        actions.add(new BATestScriptAction(false, URI.create(WON_BA.MESSAGE_EXIT.getURI()), URI.create(WON_BA.STATE_ENDED.getURI())));
        return actions;
    }
}