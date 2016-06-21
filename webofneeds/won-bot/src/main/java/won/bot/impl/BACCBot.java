package won.bot.impl;

import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsTextBots.*;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsUriBots.*;
import won.protocol.model.FacetType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 26.2.14.
 * Time: 15.15
 * To change this template use File | Settings | File Templates.
 */
public class BACCBot extends BABaseBot {


  @Override
  protected FacetType getParticipantFacetType() {
    return FacetType.BACCParticipantFacet;
  }

  @Override
  protected FacetType getCoordinatorFacetType() {
    return FacetType.BACCCoordinatorFacet;
  }

  @Override
  protected List<BATestBotScript> getScripts() {
    //add a listener that auto-responds to messages by a message
    //after NO_OF_MESSAGES messages, it unsubscribes from all events
    //subscribe it to:
    // * message events - so it responds
    // * open events - so it initiates the chain reaction of responses
    List<BATestBotScript> scripts = new ArrayList<BATestBotScript>(28);

    //Coordination message is sent as TEXT
    scripts.add(new BACCStateExitBot());
    scripts.add(new BACCStateCompensateBot());
    scripts.add(new BACCStateCompleteBot());
    scripts.add(new BACCStateCompensateFailBot());
    scripts.add(new BACCStateCompleteFailBot());
    scripts.add(new BACCStateCompleteCancelBot());
    scripts.add(new BACCStateCompleteCancelFailBot());
    scripts.add(new BACCStateActiveCancelBot());
    scripts.add(new BACCStateActiveCancelFailBot());
    scripts.add(new BACCStateCompleteExitBot());
    scripts.add(new BACCStateActiveCannotCompleteBot());
    scripts.add(new BACCStateActiveFailBot());
    scripts.add(new BACCStateCompleteCannotCompleteBot());


    //Coordination message is sent as MODEL
    scripts.add(new BACCStateExitUriBot());
    scripts.add(new BACCStateCompensateUriBot());
    scripts.add(new BACCStateCompleteUriBot());
    scripts.add(new BACCStateCompensateFailUriBot());
    scripts.add(new BACCStateCompleteFailUriBot());
    scripts.add(new BACCStateCompleteCancelUriBot());
    scripts.add(new BACCStateCompleteCancelFailUriBot());
    scripts.add(new BACCStateActiveCancelUriBot());
    scripts.add(new BACCStateActiveCancelFailUriBot());
    scripts.add(new BACCStateCompleteExitUriBot());
    scripts.add(new BACCStateActiveCannotCompleteUriBot());
    scripts.add(new BACCStateActiveFailUriBot());
    scripts.add(new BACCStateCompleteCannotCompleteUriBot());


    // with failures
 //   scripts.add(new BACCStateCompleteWithFailuresUriBot());
 //   scripts.add(new BACCStateCompleteWithFailuresBot());
    return scripts;
  }
}

