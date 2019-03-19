package won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsTextBots;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.BATestScriptAction;
import won.node.facet.impl.WON_TX;

/**
 * Created with IntelliJ IDEA. User: Danijel Date: 13.3.14. Time: 16.42 To change this template use File | Settings |
 * File Templates.
 */
public class BACCStateFailureOne extends BATestBotScript {

    @Override
    protected List<BATestScriptAction> setupActions() {
        List<BATestScriptAction> actions = new ArrayList();
        actions.add(
                new BATestScriptAction(true, "MESSAGE_CANCELED", URI.create(WON_TX.STATE_CANCELING_ACTIVE.getURI()))); // Message
                                                                                                                       // can
                                                                                                                       // be
                                                                                                                       // sent
                                                                                                                       // from
                                                                                                                       // Participant,
                                                                                                                       // but
                                                                                                                       // not
                                                                                                                       // from
                                                                                                                       // this
                                                                                                                       // state
        actions.add(new BATestScriptAction(true, "MESSAGE_CANCELED", URI.create(WON_TX.STATE_ACTIVE.getURI()))); // Message
                                                                                                                 // can
                                                                                                                 // not
                                                                                                                 // be
                                                                                                                 // sent
                                                                                                                 // from
                                                                                                                 // Participant,
                                                                                                                 // only
                                                                                                                 // from
                                                                                                                 // Coordinator
        // da je false ne bi radilo!!! ispitaj!!!
        actions.add(new BATestScriptAction(true, "MESSAGE_CANCELED", URI.create(WON_TX.STATE_ACTIVE.getURI()))); // Message
                                                                                                                 // can
                                                                                                                 // not
                                                                                                                 // be
                                                                                                                 // sent
                                                                                                                 // from
                                                                                                                 // Participant,
                                                                                                                 // only
                                                                                                                 // from
                                                                                                                 // Coordinator
        actions.add(new BATestScriptAction(false, "MESSAGE_CANCEL", URI.create(WON_TX.STATE_ACTIVE.getURI())));
        actions.add(
                new BATestScriptAction(true, "MESSAGE_CANCELED", URI.create(WON_TX.STATE_CANCELING_ACTIVE.getURI())));

        // actions.add(new BATestScriptAction(false, "MESSAGE_CANCEL", URI.create(WON_BA.STATE_ACTIVE.getURI())));
        // //Message can not be sent from Participant, only from Coordinator
        // actions.add(new BATestScriptAction(false, "MESSAGE_EXITED", URI.create(WON_BA.STATE_ACTIVE.getURI())));
        // //Message can be sent from Coordinator, but not from this state
        // actions.add(new BATestScriptAction(false, "MESSAGE_COMPLETED", URI.create(WON_BA.STATE_ACTIVE.getURI())));
        // actions.add(new BATestScriptAction(true, "MESSAGE_COMPLETED", URI.create(WON_BA.STATE_ACTIVE.getURI())));
        //
        // actions.add(new BATestScriptAction(false, "MESSAGE_CANCEL", URI.create(WON_BA.STATE_ACTIVE.getURI()))); //ok
        // actions.add(new BATestScriptAction(true, "MESSAGE_CANCELED",
        // URI.create(WON_BA.STATE_CANCELING_ACTIVE.getURI()))); //Message can be sent from Participant, but not from
        // this state
        // actions.add(new BATestScriptAction(true, "MESSAGE_CANCEL", URI.create(WON_BA.STATE_ACTIVE.getURI())));
        // //Message can not be sent from Participant, only from Coordinator
        // actions.add(new BATestScriptAction(false, "MESSAGE_CANCEL", URI.create(WON_BA.STATE_ACTIVE.getURI())));
        // //Message can not be sent from Participant, only from Coordinator
        // actions.add(new BATestScriptAction(false, "MESSAGE_EXITED", URI.create(WON_BA.STATE_ACTIVE.getURI())));
        // //Message can be sent from Coordinator, but not from this state
        // actions.add(new BATestScriptAction(false, "MESSAGE_COMPLETED", URI.create(WON_BA.STATE_ACTIVE.getURI())));
        // actions.add(new BATestScriptAction(true, "MESSAGE_COMPLETED", URI.create(WON_BA.STATE_ACTIVE.getURI())));
        // actions.add(new BATestScriptAction(true, "MESSAGE_CANCELED",
        // URI.create(WON_BA.STATE_CANCELING_ACTIVE.getURI())));
        return actions;
    }
}