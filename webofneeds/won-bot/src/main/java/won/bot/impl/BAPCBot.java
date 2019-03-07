package won.bot.impl;

import java.util.ArrayList;
import java.util.List;

import won.bot.framework.eventbot.listener.baStateBots.BATestBotScript;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.coordinationMessageAsTextBots.BAPCStateActiveCancelBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.coordinationMessageAsTextBots.BAPCStateActiveCancelFailBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.coordinationMessageAsTextBots.BAPCStateActiveCannotCompleteBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.coordinationMessageAsTextBots.BAPCStateActiveFailBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.coordinationMessageAsTextBots.BAPCStateCompensateBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.coordinationMessageAsTextBots.BAPCStateCompensateFailBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.coordinationMessageAsTextBots.BAPCStateCompleteBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.coordinationMessageAsTextBots.BAPCStateExitBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.coordinationMessageAsUriBots.BAPCStateActiveCancelFailUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.coordinationMessageAsUriBots.BAPCStateActiveCancelUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.coordinationMessageAsUriBots.BAPCStateActiveCannotCompleteUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.coordinationMessageAsUriBots.BAPCStateActiveFailUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.coordinationMessageAsUriBots.BAPCStateCompensateFailUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.coordinationMessageAsUriBots.BAPCStateCompensateUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.coordinationMessageAsUriBots.BAPCStateCompleteUriBot;
import won.bot.framework.eventbot.listener.baStateBots.baPCMessagingBots.coordinationMessageAsUriBots.BAPCStateExitUriBot;
import won.protocol.model.FacetType;


/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 12.2.14.
 * Time: 20.45
 * To change this template use File | Settings | File Templates.
 */
public class BAPCBot extends BABaseBot {


  @Override
  protected FacetType getParticipantFacetType() {
    return FacetType.BAPCParticipantFacet;
  }

  @Override
  protected FacetType getCoordinatorFacetType() {
    return FacetType.BAPCCoordinatorFacet;
  }
  @Override
  protected List<BATestBotScript> getScripts() {
    //add a listener that auto-responds to messages by a message
    //after NO_OF_MESSAGES messages, it unsubscribes from all events
    //subscribe it to:
    // * message events - so it responds
    // * open events - so it initiates the chain reaction of responses
    List<BATestBotScript> scripts = new ArrayList<BATestBotScript>(18);

    //Coordination message is sent as TEXT
    scripts.add(new BAPCStateExitBot());
    scripts.add(new BAPCStateCompleteBot());
    scripts.add(new BAPCStateCompensateBot());
    scripts.add(new BAPCStateCompensateFailBot());
    scripts.add(new BAPCStateActiveFailBot());
    scripts.add(new BAPCStateActiveCancelBot());
    scripts.add(new BAPCStateActiveCancelFailBot());
    scripts.add(new BAPCStateActiveCannotCompleteBot());

    //Coordination message is sent as MODEL
    scripts.add(new BAPCStateExitUriBot());
    scripts.add(new BAPCStateCompleteUriBot());
    scripts.add(new BAPCStateCompensateUriBot());
    scripts.add(new BAPCStateCompensateFailUriBot());
    scripts.add(new BAPCStateActiveFailUriBot());
    scripts.add(new BAPCStateActiveCancelUriBot());
    scripts.add(new BAPCStateActiveCancelFailUriBot());
    scripts.add(new BAPCStateActiveCannotCompleteUriBot());

    //with failures
   // scripts.add(new BAPCStateCompleteWithFailureUriBot());
   // scripts.add(new BAPCStateCompleteWithFailureBot());
    return scripts;
  }
}

