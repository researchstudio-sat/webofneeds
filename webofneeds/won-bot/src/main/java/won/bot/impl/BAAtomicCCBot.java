package won.bot.impl;

import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsTextBot.CompletedBlockedFPBot;
import won.bot.framework.events.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsTextBot.CompletedBlockedSPBot;
import won.bot.framework.events.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsTextBot.CompletingBlockingFPBot;
import won.bot.framework.events.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsTextBot.CompletingBlockingSPBot;
import won.bot.framework.events.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsUriBot.CompletedBlockedFPUriBot;
import won.bot.framework.events.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsUriBot.CompletedBlockedSPUriBot;
import won.protocol.model.FacetType;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Danijel
 * Date: 24.4.14.
 */
public class BAAtomicCCBot extends BAAtomicBaseBot
{
  @Override
  protected FacetType getParticipantFacetType() {
    return FacetType.BACCParticipantFacet;
  }

  @Override
  protected FacetType getCoordinatorFacetType() {
    return FacetType.BAAtomicCCCoordinatorFacet;
  }

  protected List<BATestBotScript> getFirstPhaseScripts() {

    List<BATestBotScript> scripts = new ArrayList<BATestBotScript>(3);

    //Coordination message is sent as TEXT
    scripts.add(new CompletingBlockingFPBot());
    scripts.add(new CompletedBlockedFPBot());

   //Coordination message is sent as MODEL
    scripts.add(new CompletedBlockedFPUriBot());

    return scripts;
  }

  protected List<BATestBotScript> getSecondPhaseScripts() {
    List<BATestBotScript> scripts = new ArrayList<BATestBotScript>(3);

    scripts.add(new CompletingBlockingSPBot());
    scripts.add(new CompletedBlockedSPBot());


    //Coordination message is sent as MODEL
    scripts.add(new CompletedBlockedSPUriBot());

    return scripts;
  }
}
