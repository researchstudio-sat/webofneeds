/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.bot.impl;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.action.impl.*;
import won.bot.framework.events.action.impl.debugbot.*;
import won.bot.framework.events.action.impl.matcher.RegisterMatcherAction;
import won.bot.framework.events.action.impl.needlifecycle.DeactivateNeedAction;
import won.bot.framework.events.action.impl.wonmessage.*;
import won.bot.framework.events.bus.EventBus;
import won.bot.framework.events.event.impl.command.SendTextMessageOnConnectionEvent;
import won.bot.framework.events.event.impl.debugbot.*;
import won.bot.framework.events.event.impl.lifecycle.ActEvent;
import won.bot.framework.events.event.impl.matcher.NeedCreatedEventForMatcher;
import won.bot.framework.events.event.impl.wonmessage.*;
import won.bot.framework.events.filter.impl.NeedUriInNamedListFilter;
import won.bot.framework.events.filter.impl.NotFilter;
import won.bot.framework.events.listener.BaseEventListener;
import won.bot.framework.events.listener.impl.ActionOnEventListener;
import won.protocol.model.FacetType;

import java.net.URI;

/**
 * Bot that reacts to each new need that is created in the system by creating two needs, it sends a connect message from
 * one of these needs, and a hint message for original need offering match to another of these needs. Additionally,
 * it reacts to certain commands send via text messages on the connections with the created by the bot needs.
 */
public class DebugBot extends EventBot
{

  private static final String NAME_NEEDS = "debugNeeds";
  private static final long CONNECT_DELAY_MILLIS = 0;
  private static final long DELAY_BETWEEN_N_MESSAGES = 1000;
  private static final double CHATTY_MESSAGE_PROBABILITY = 0.1;
  private BaseEventListener matcherRegistrator;
  protected BaseEventListener needCreator;
  protected BaseEventListener needConnector;
  protected BaseEventListener needHinter;
  protected BaseEventListener autoOpener;
  protected BaseEventListener needCloser;
  protected BaseEventListener needDeactivator;
  protected BaseEventListener messageFromOtherNeedListener;
  protected BaseEventListener usageMessageSender;


  private URI matcherUri;

  public void setMatcherUri(final URI matcherUri) {
    this.matcherUri = matcherUri;
  }

  @Override
  protected void initializeEventListeners()
  {
    String welcomeMessage = "Greetings! \nI am the DebugBot. I " +
      "can simulate multiple other users so you can test things. I understand a few commands. \nTo see which ones, " +
      "type \n\n'usage'\n\n (without the quotes).";

    EventListenerContext ctx = getEventListenerContext();
    EventBus bus = getEventBus();

    //register with WoN noodes, be notified when new needs are created
    this.matcherRegistrator = new ActionOnEventListener(
      ctx,
      new RegisterMatcherAction(ctx),
      1
    );
    bus.subscribe(ActEvent.class, this.matcherRegistrator);

    //create the echo need for debug initial connect - if we're not reacting to the creation of our own echo need.
    CreateDebugNeedWithFacetsAction needForInitialConnectAction = new CreateDebugNeedWithFacetsAction(ctx, NAME_NEEDS);
    needForInitialConnectAction.setIsInitialForConnect(true);

    ActionOnEventListener initialConnector = new ActionOnEventListener(
            ctx,
            new NotFilter(new NeedUriInNamedListFilter(ctx, NAME_NEEDS)),
            needForInitialConnectAction
            );
    bus.subscribe(NeedCreatedEventForMatcher.class, initialConnector);

    //create the echo need for debug initial hint - if we're not reacting to the creation of our own echo need.
    CreateDebugNeedWithFacetsAction initialHinter = new CreateDebugNeedWithFacetsAction(ctx, NAME_NEEDS);
    initialHinter.setIsInitialForHint(true);
    ActionOnEventListener needForInitialHintListener = new ActionOnEventListener(
      ctx, new NotFilter(new NeedUriInNamedListFilter(ctx, NAME_NEEDS)), initialHinter);
    bus.subscribe(NeedCreatedEventForMatcher.class, needForInitialHintListener);

    //as soon as the echo need triggered by debug connect created, connect to original
    this.needConnector = new ActionOnEventListener(
      ctx,
      "needConnector",
      new RandomDelayedAction(ctx, CONNECT_DELAY_MILLIS,CONNECT_DELAY_MILLIS,1,
                                new ConnectWithAssociatedNeedAction(ctx,
                                                                    FacetType.OwnerFacet.getURI(),
                                                                    FacetType.OwnerFacet.getURI(),
                                                                    welcomeMessage)));
    bus.subscribe(NeedCreatedEventForDebugConnect.class, this.needConnector);

    //as soon as the echo need triggered by debug hint command created, hint to original
    this.needHinter = new ActionOnEventListener(
      ctx,
      "needHinter",
      new RandomDelayedAction(ctx, CONNECT_DELAY_MILLIS,CONNECT_DELAY_MILLIS,1,
                              new HintAssociatedNeedAction(ctx, FacetType.OwnerFacet.getURI(), FacetType.OwnerFacet.getURI(), matcherUri)
      ));
    bus.subscribe(NeedCreatedEventForDebugHint.class, this.needHinter);


    //if the original need wants to connect - always open
    this.autoOpener = new ActionOnEventListener(ctx,
                                                new  MultipleActions(ctx,
                                                                     new OpenConnectionAction(ctx, welcomeMessage ),
                                                                     new PublishSetChattinessEventAction(ctx, true)));
    bus.subscribe(ConnectFromOtherNeedEvent.class, this.autoOpener);

    //if the remote side opens, send a greeting and set to chatty.
    bus.subscribe(OpenFromOtherNeedEvent.class,
                  new ActionOnEventListener(ctx,
                                            new MultipleActions(ctx,
                                                                new RespondToMessageAction(ctx, "Hi there!"),
                                                                new PublishSetChattinessEventAction(ctx, true))));

    //if the bot receives a text message - try to map the command of the text message to a DebugEvent
    messageFromOtherNeedListener = new ActionOnEventListener(ctx, new DebugBotIncomingMessageToEventMappingAction(ctx));
    bus.subscribe(MessageFromOtherNeedEvent.class, messageFromOtherNeedListener);

    //react to usage command event
    this.usageMessageSender = new ActionOnEventListener(ctx, new SendMultipleMessagesAction(
      ctx,DebugBotIncomingMessageToEventMappingAction.USAGE_MESSAGES));
    bus.subscribe(UsageDebugCommandEvent.class, usageMessageSender);


    //react to the debug close command (close the connection)
    this.needCloser = new ActionOnEventListener(ctx,
                                                new MultipleActions(ctx,
                                                                    new CloseConnectionAction(ctx, "As per your" +
                                                                    " request, this " +
                                                                      "connection is being closed."),
                                                                    new PublishSetChattinessEventAction(ctx, false)));

    bus.subscribe(CloseDebugCommandEvent.class, this.needCloser);

    //react to the debug deactivate command (deactivate my need)
    this.needDeactivator = new ActionOnEventListener(ctx,new DeactivateNeedAction(ctx));
    bus.subscribe(DeactivateDebugCommandEvent.class, this.needDeactivator);

    //react to close event: set connection to not chatty
    bus.subscribe(CloseFromOtherNeedEvent.class,
                  new ActionOnEventListener(ctx,
                                            new PublishSetChattinessEventAction(ctx, false)));

    // react to the hint and connect commands by creating a need (it will fire correct need created for connect/hint
    // events)
    needCreator = new ActionOnEventListener(
      ctx,
      new CreateDebugNeedWithFacetsAction(ctx,NAME_NEEDS)
    );
    bus.subscribe(HintDebugCommandEvent.class, needCreator);
    bus.subscribe(ConnectDebugCommandEvent.class, needCreator);

    //react to a message that was not identified as a debug command
    bus.subscribe(SendTextMessageOnConnectionEvent.class, new ActionOnEventListener(ctx, new
      SendMessageOnConnectionAction(ctx)));

    bus.subscribe(SendNDebugCommandEvent.class,
                  new ActionOnEventListener(ctx, new SendNDebugMessagesAction(ctx,
                  DELAY_BETWEEN_N_MESSAGES, DebugBotIncomingMessageToEventMappingAction.N_MESSAGES)));

    MessageTimingManager timingManager = new MessageTimingManager(ctx, 20);

    //on every actEvent there is a chance we send a chatty message
    bus.subscribe(ActEvent.class, new ActionOnEventListener(ctx, new SendChattyMessageAction(ctx,
                                                                                             CHATTY_MESSAGE_PROBABILITY,
                                                                                             timingManager,
                                                                                                DebugBotIncomingMessageToEventMappingAction.RANDOM_MESSAGES,
                                                                                                DebugBotIncomingMessageToEventMappingAction.LAST_MESSAGES) ));
    //set the chattiness of the connection
    bus.subscribe(SetChattinessDebugCommandEvent.class, new ActionOnEventListener(ctx, new SetChattinessAction(ctx)));

    //process eliza messages with eliza
    bus.subscribe(MessageToElizaEvent.class, new ActionOnEventListener(ctx, new AnswerWithElizaAction(ctx,20)));

    //remember when we sent the last message
    bus.subscribe(WonMessageSentOnConnectionEvent.class,
                  new ActionOnEventListener(ctx, new RecordMessageSentTimeAction(ctx, timingManager)));
    //remember when we got the last message
    bus.subscribe(WonMessageReceivedOnConnectionEvent.class,
                  new ActionOnEventListener(ctx, new RecordMessageReceivedTimeAction(ctx, timingManager)));
    //initialize the sent timestamp when the open message is received
    bus.subscribe(OpenFromOtherNeedEvent.class,
                  new ActionOnEventListener(ctx, new RecordMessageSentTimeAction(ctx, timingManager)));
    //initialize the sent timestamp when the connect message is received
    bus.subscribe(ConnectFromOtherNeedEvent.class,
                  new ActionOnEventListener(ctx, new RecordMessageSentTimeAction(ctx, timingManager)));

  }


}
