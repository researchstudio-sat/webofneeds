package won.bot.impl;

import won.bot.framework.events.listener.baStateBots.BATestBotScript;
import won.bot.framework.events.listener.baStateBots.baCCMessagingBots.atomicBots.activeBots.coordinationMessageAsTextBot.*;
import won.bot.framework.events.listener.baStateBots.baCCMessagingBots.atomicBots.activeBots.coordinationMessageAsUriBot.*;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Danijel
 * Date: 10.4.14.
 */
public abstract class BAAtomicCCActiveExitingBot extends BAAtomicCCBaseBot{
  @Override
  protected List<BATestBotScript> getFirstPhaseScripts() {

    List<BATestBotScript> scripts = new ArrayList<BATestBotScript>(13);

    //Coordination message is sent as TEXT
    scripts.add(new ActiveNoVoteFPBot());
    scripts.add(new CompletingFPBot());
    scripts.add(new CompletingFPBot());
    scripts.add(new CompletedFPBot());
    scripts.add(new CompletedFPBot());
    scripts.add(new ActiveFPBot());
    scripts.add(new ActiveFPBot());
//
//    //Coordination message is sent as MODEL
    scripts.add(new CompletingFPUriBot());
    scripts.add(new CompletingFPUriBot());
    scripts.add(new CompletedFPUriBot());
    scripts.add(new CompletedFPUriBot());
    scripts.add(new ActiveFPUriBot());
    scripts.add(new ActiveFPUriBot());

    return scripts;
  }

  protected List<BATestBotScript> getSecondPhaseScripts() {
    List<BATestBotScript> scripts = new ArrayList<BATestBotScript>(13);

   // scripts.add(new ActiveExitVoteSPBot());
   //scripts.add(new ActiveFailingVoteSPBot());
    scripts.add(new ActiveCannotCompleteVoteSPBot());
    scripts.add(new CompletingSPCancelingBot());
    scripts.add(new CompletingSPCancelingFailingBot());
    scripts.add(new CompletedSPCompensatingBot());
    scripts.add(new CompletedSPCompensatingFailingBot());
    scripts.add(new ActiveSPCancelingBot());
    scripts.add(new ActiveSPCancelingFailingBot());
//
//    //Coordination message is sent as MODEL
    scripts.add(new CompletingSPCancelingUriBot());
    scripts.add(new CompletingSPCancelingFailingUriBot());
    scripts.add(new CompletedSPCompensatingFailingUriBot());
    scripts.add(new CompletedSPCompensatingFailingUriBot());
    scripts.add(new ActiveSPCancelingUriBot());
    scripts.add(new ActiveSPCancelingFailingUriBot());

    return scripts;
  }
}
