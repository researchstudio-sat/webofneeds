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
import won.bot.framework.events.bus.EventBus;
import won.bot.framework.events.event.impl.*;
import won.bot.framework.events.event.impl.debugbot.*;
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
  private static final long CONNECT_DELAY_MILLIS = 5000;
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
    EventListenerContext ctx = getEventListenerContext();
    EventBus bus = getEventBus();

    //register with WoN noodes, be notified when new needs are created
    this.matcherRegistrator = new ActionOnEventListener(
      ctx,
      new RegisterMatcherAction(ctx),
      1
    );
    bus.subscribe(ActEvent.class,this.matcherRegistrator);

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

    //if the original need wants to connect - always open
    this.autoOpener = new ActionOnEventListener(ctx,new OpenConnectionAction(ctx));
    bus.subscribe(ConnectFromOtherNeedEvent.class, this.autoOpener);


    //as soon as the echo need triggered by debug connect created, connect to original
    this.needConnector = new ActionOnEventListener(
      ctx,
      "needConnector",
      new RandomDelayedAction(ctx, CONNECT_DELAY_MILLIS,CONNECT_DELAY_MILLIS,1,
            new ConnectWithAssociatedNeedAction(ctx,FacetType.OwnerFacet.getURI(),FacetType.OwnerFacet.getURI())
                        ));
    bus.subscribe(NeedCreatedEventForDebugConnect.class, this.needConnector);


    //as soon as the echo need triggered by debug hint command created, hint to original
    this.needHinter = new ActionOnEventListener(
      ctx,
      "needHinter",
      new RandomDelayedAction(ctx, CONNECT_DELAY_MILLIS,CONNECT_DELAY_MILLIS,1,
            new HintAssociatedNeedAction(ctx,FacetType.OwnerFacet.getURI(),FacetType.OwnerFacet.getURI(), matcherUri)
      ));
    bus.subscribe(NeedCreatedEventForDebugHint.class, this.needHinter);

    //if the bot receives a text message - try to map the command of the text message to a DebugEvent
    messageFromOtherNeedListener = new ActionOnEventListener(ctx, new DebugBotIncomingMessageToEventMappingAction(ctx));
    bus.subscribe(MessageFromOtherNeedEvent.class, messageFromOtherNeedListener);

    //react to usage command event
    this.usageMessageSender = new ActionOnEventListener(ctx, new SendMessageAction(
      ctx,DebugBotIncomingMessageToEventMappingAction.USAGE_MESSAGE));
    bus.subscribe(UsageDebugCommandEvent.class, usageMessageSender);


    //react to the debug close command (close the connection)
    this.needCloser = new ActionOnEventListener(ctx,new CloseConnectionAction(ctx));
    bus.subscribe(CloseDebugCommandEvent.class, this.needCloser);

    //react to the debug deactivate command (deactivate my need)
    this.needDeactivator = new ActionOnEventListener(ctx,new DeactivateNeedAction(ctx));
    bus.subscribe(DeactivateDebugCommandEvent.class, this.needDeactivator);


    // react to the hint and connect commands by creating a need (it will fire correct need created for connect/hint
    // events)
    needCreator = new ActionOnEventListener(
      ctx,
      new CreateDebugNeedWithFacetsAction(ctx,NAME_NEEDS)
    );
    bus.subscribe(HintDebugCommandEvent.class, needCreator);
    bus.subscribe(ConnectDebugCommandEvent.class, needCreator);

  }


}
