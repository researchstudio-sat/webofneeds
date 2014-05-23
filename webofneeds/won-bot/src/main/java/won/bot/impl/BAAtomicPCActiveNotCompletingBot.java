package won.bot.impl;

import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsTextBot.*;
import won.bot.framework.events.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsUriBot.*;
import won.protocol.model.FacetType;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Danijel
 * Date: 24.4.14.
 */
public class BAAtomicPCActiveNotCompletingBot extends BAAtomicBaseBot
{
  @Override
  protected FacetType getParticipantFacetType() {
    return FacetType.BAPCParticipantFacet;
  }

  @Override
  protected FacetType getCoordinatorFacetType() {
    return FacetType.BAAtomicPCCoordinatorFacet;
  }

  protected List<BATestBotScript> getFirstPhaseScripts() {

    List<BATestBotScript> scripts = new ArrayList<BATestBotScript>(9);

    //Coordination message is sent as TEXT
    scripts.add(new ActiveFPBot());
    scripts.add(new CompletedFPBot());
    scripts.add(new CompletedFPBot());
    scripts.add(new ActiveFPBot());
    scripts.add(new ActiveFPBot());
//
//    //Coordination message is sent as MODEL
    scripts.add(new CompletedFPUriBot());
    scripts.add(new CompletedFPUriBot());
    scripts.add(new ActiveFPUriBot());
    scripts.add(new ActiveFPUriBot());

    return scripts;
  }

  protected List<BATestBotScript> getSecondPhaseScripts() {
    List<BATestBotScript> scripts = new ArrayList<BATestBotScript>(9);

    scripts.add(new ActiveCannotCompleteVoteSPBot());
    scripts.add(new CompletedSPCompensatingBot());
    scripts.add(new CompletedSPCompensatingFailingBot());
    scripts.add(new ActiveSPCancelingBot());
    scripts.add(new ActiveSPCancelingFailingBot());
//
//    //Coordination message is sent as MODEL
    scripts.add(new CompletedSPCompensatingFailingUriBot());
    scripts.add(new CompletedSPCompensatingFailingUriBot());
    scripts.add(new ActiveSPCancelingUriBot());
    scripts.add(new ActiveSPCancelingFailingUriBot());

    return scripts;
  }
}