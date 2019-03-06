package won.bot.impl;

import java.util.ArrayList;
import java.util.List;

import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsTextBot.ActiveExitVoteSPBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsTextBot.ActiveFPBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsTextBot.ActiveSPCancelingBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsTextBot.ActiveSPCancelingFailingBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsTextBot.CompletedFPBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsTextBot.CompletedSPCompensatingBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsTextBot.CompletedSPCompensatingFailingBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsUriBot.ActiveFPUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsUriBot.ActiveSPCancelingFailingUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsUriBot.ActiveSPCancelingUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsUriBot.CompletedFPUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.atomicBots.coordinationMessageAsUriBot.CompletedSPCompensatingFailingUriBot;
import won.protocol.model.FacetType;

/**
 * User: Danijel
 * Date: 24.4.14.
 */
public class BAAtomicPCActiveExitingBot  extends BAAtomicBaseBot
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

    scripts.add(new ActiveExitVoteSPBot());
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

