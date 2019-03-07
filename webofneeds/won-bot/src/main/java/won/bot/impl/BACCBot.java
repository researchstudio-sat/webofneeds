package won.bot.impl;

import java.util.ArrayList;
import java.util.List;

import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsTextBots.BACCStateActiveCancelBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsTextBots.BACCStateActiveCancelFailBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsTextBots.BACCStateActiveCannotCompleteBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsTextBots.BACCStateActiveFailBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsTextBots.BACCStateCompensateBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsTextBots.BACCStateCompensateFailBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsTextBots.BACCStateCompleteBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsTextBots.BACCStateCompleteCancelBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsTextBots.BACCStateCompleteCancelFailBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsTextBots.BACCStateCompleteCannotCompleteBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsTextBots.BACCStateCompleteExitBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsTextBots.BACCStateCompleteFailBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsTextBots.BACCStateExitBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsUriBots.BACCStateActiveCancelFailUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsUriBots.BACCStateActiveCancelUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsUriBots.BACCStateActiveCannotCompleteUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsUriBots.BACCStateActiveFailUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsUriBots.BACCStateCompensateFailUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsUriBots.BACCStateCompensateUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsUriBots.BACCStateCompleteCancelFailUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsUriBots.BACCStateCompleteCancelUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsUriBots.BACCStateCompleteCannotCompleteUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsUriBots.BACCStateCompleteExitUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsUriBots.BACCStateCompleteFailUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsUriBots.BACCStateCompleteUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baCCMessagingBots.coordinationMessageAsUriBots.BACCStateExitUriBot;
import won.protocol.model.FacetType;

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

