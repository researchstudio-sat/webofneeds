package won.bot.impl;

import java.util.ArrayList;
import java.util.List;

import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsTextBot.CompletedFPBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.atomicBots.coordinationMessageAsTextBot.CompletedSPClosingAdditionalParticipantsBot;
import won.protocol.model.FacetType;

/**
 * User: Danijel
 * Date: 7.5.14.
 */
public class BAAtomicCCAdditionalParticipants extends BAAtomicAdditionalParticipantsBaseBot
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

    List<BATestBotScript> scripts = new ArrayList<BATestBotScript>(2);

    //Coordination message is sent as TEXT
    scripts.add(new CompletedFPBot());
    scripts.add(new CompletedFPBot());

//
//    //Coordination message is sent as MODEL
 //   scripts.add(new CompletingFPUriBot());
 //   scripts.add(new CompletingFPUriBot());

    return scripts;
  }

  protected List<BATestBotScript> getFirstPhaseScriptsWithDelay() {
    List<BATestBotScript> scripts = new ArrayList<BATestBotScript>(2);

    scripts.add(new CompletedFPBot());
    scripts.add(new CompletedFPBot());
//
//    //Coordination message is sent as MODEL
    // scripts.add(new CompletingFPUriBot());
    // scripts.add(new CompletingFPUriBot());

    return scripts;
  }

  protected List<BATestBotScript> getSecondPhaseScripts() {
    List<BATestBotScript> scripts = new ArrayList<BATestBotScript>(4);

    scripts.add(new CompletedSPClosingAdditionalParticipantsBot("CompletedSPClosingBot-1"));
    scripts.add(new CompletedSPClosingAdditionalParticipantsBot("CompletedSPClosingBot-2"));
    scripts.add(new CompletedSPClosingAdditionalParticipantsBot("CompletedSPClosingBot-3"));
    scripts.add(new CompletedSPClosingAdditionalParticipantsBot("CompletedSPClosingBot-4"));

//
//    //Coordination message is sent as MODEL
    //scripts.add(new CompletedSPClosingUri;    TODO


    return scripts;
  }

}
