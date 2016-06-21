package won.bot.impl;

import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsTextBot.*;
import won.protocol.model.FacetType;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Danijel
 * Date: 10.4.14.
 */
public class BAAtomicCCActiveExitingBot extends BAAtomicBaseBot
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
    scripts.add(new ActiveFPBot());
    scripts.add(new CompletingFPBot());
    scripts.add(new CompletingFPBot());
//    scripts.add(new CompletedFPBot());
//    scripts.add(new CompletedFPBot());
//    scripts.add(new ActiveFPBot());
//    scripts.add(new ActiveFPBot());
//
//    //Coordination message is sent as MODEL
//   scripts.add(new CompletingFPUriBot());
//    scripts.add(new CompletingFPUriBot());
//    scripts.add(new CompletedFPUriBot());
//    scripts.add(new CompletedFPUriBot());
//    scripts.add(new ActiveFPUriBot());
//    scripts.add(new ActiveFPUriBot());

    return scripts;
  }

  protected List<BATestBotScript> getSecondPhaseScripts() {
    List<BATestBotScript> scripts = new ArrayList<BATestBotScript>(3);

    scripts.add(new ActiveExitVoteSPBot());
    scripts.add(new CompletingSPCancelingBot());
    scripts.add(new CompletingSPCancelingFailingBot());
//    scripts.add(new CompletedSPCompensatingBot());
//    scripts.add(new CompletedSPCompensatingFailingBot());
//    scripts.add(new ActiveSPCancelingBot());
//    scripts.add(new ActiveSPCancelingFailingBot());
//
//    //Coordination message is sent as MODEL
//    scripts.add(new CompletingSPCancelingUriBot());
//    scripts.add(new CompletingSPCancelingFAilingUriBot());
//    scripts.add(new CompletedSPCCompensatingUriBot());
//    scripts.add(new CompletedSPCompensatingFailingUriBot());
//    scripts.add(new ActiveSPCancelingUriBot());
//    scripts.add(new ActiveSPCancelingFailingUriBot());

    return scripts;
  }
}
