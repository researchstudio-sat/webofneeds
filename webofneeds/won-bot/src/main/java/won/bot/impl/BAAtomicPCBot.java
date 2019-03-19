package won.bot.impl;

import java.util.ArrayList;
import java.util.List;

import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsTextBot.ActiveBlockingSPBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsTextBot.CompletedBlockedFPBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsTextBot.CompletedBlockedSPBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsTextBot.CompletedFPBot;
import won.protocol.model.FacetType;

/**
 * User: Danijel Date: 24.4.14.
 */
public class BAAtomicPCBot extends BAAtomicBaseBot {
    @Override
    protected FacetType getParticipantFacetType() {
        return FacetType.BAPCParticipantFacet;
    }

    @Override
    protected FacetType getCoordinatorFacetType() {
        return FacetType.BAAtomicPCCoordinatorFacet;
    }

    protected List<BATestBotScript> getFirstPhaseScripts() {

        List<BATestBotScript> scripts = new ArrayList<BATestBotScript>(2);

        // Coordination message is sent as TEXT
        scripts.add(new CompletedFPBot());
        scripts.add(new CompletedBlockedFPBot());

        // Coordination message is sent as MODEL

        return scripts;
    }

    protected List<BATestBotScript> getSecondPhaseScripts() {
        List<BATestBotScript> scripts = new ArrayList<BATestBotScript>(2);

        // Coordination message is sent as TEXT
        scripts.add(new ActiveBlockingSPBot());
        scripts.add(new CompletedBlockedSPBot());

        // Coordination message is sent as MODEL

        return scripts;
    }
}
