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

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.RandomDelayedAction;
import won.bot.framework.eventbot.action.impl.matcher.RegisterMatcherAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.lifecycle.ActEvent;
import won.bot.framework.eventbot.event.impl.matcher.MatcherRegisterFailedEvent;
import won.bot.framework.eventbot.event.impl.matcher.NeedCreatedEventForMatcher;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

/**
 * Bot that connects the two last seen needs using a hint.
 */
public class LastSeenNeedsMatcherBot extends EventBot {

  private BaseEventListener matcherRegistrator;
  private BaseEventListener matcherIndexer;
  private int registrationMatcherRetryInterval;

  public void setRegistrationMatcherRetryInterval(final int registrationMatcherRetryInterval) {
    this.registrationMatcherRetryInterval = registrationMatcherRetryInterval;
  }

  private URI matcherUri;

  public void setMatcherUri(final URI matcherUri) {
    this.matcherUri = matcherUri;
  }

  // we remember the need uri each time a new need is encountered
  private AtomicReference<URI> lastNeedUriReference = new AtomicReference<>();

  @Override
  protected void initializeEventListeners() {
    EventListenerContext ctx = getEventListenerContext();
    EventBus bus = getEventBus();

    // subscribe this bot with the WoN nodes' 'new need' topic
    RegisterMatcherAction registerMatcherAction = new RegisterMatcherAction(ctx);
    this.matcherRegistrator = new ActionOnEventListener(ctx, registerMatcherAction, 1);
    bus.subscribe(ActEvent.class, this.matcherRegistrator);
    RandomDelayedAction delayedRegistration = new RandomDelayedAction(ctx, registrationMatcherRetryInterval,
        registrationMatcherRetryInterval, 0, registerMatcherAction);
    ActionOnEventListener matcherRetryRegistrator = new ActionOnEventListener(ctx, delayedRegistration);
    bus.subscribe(MatcherRegisterFailedEvent.class, matcherRetryRegistrator);

    bus.subscribe(NeedCreatedEventForMatcher.class,
        new ActionOnEventListener(ctx, "lastSeenNeedsMatcher", new BaseEventBotAction(ctx) {
          @Override
          protected void doRun(final Event event, EventListener executingListener) throws Exception {
            NeedCreatedEventForMatcher needCreatedEvent = (NeedCreatedEventForMatcher) event;
            URI currentNeedURI = needCreatedEvent.getNeedURI();
            URI lastNeedURI = lastNeedUriReference.getAndSet(currentNeedURI);
            URI originator = matcherUri;
            if (lastNeedURI == null) {
              logger.info("First invocation. Remembering {} for matching it later", currentNeedURI);
              return;
            } else {
              logger.info("Sending hint for {} and {}", currentNeedURI, lastNeedURI);
            }
            ctx.getMatcherProtocolNeedServiceClient().hint(currentNeedURI, lastNeedURI, 0.5, originator, null,
                createWonMessage(currentNeedURI, lastNeedURI, 0.5, originator));
            ctx.getMatcherProtocolNeedServiceClient().hint(lastNeedURI, currentNeedURI, 0.5, originator, null,
                createWonMessage(lastNeedURI, currentNeedURI, 0.5, originator));
          }
        }));
  }

  private WonMessage createWonMessage(URI needURI, URI otherNeedURI, double score, URI originator)
      throws WonMessageBuilderException {

    WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();

    URI localWonNode = WonRdfUtils.NeedUtils
        .getWonNodeURIFromNeed(getEventListenerContext().getLinkedDataSource().getDataForResource(needURI), needURI);

    return WonMessageBuilder.setMessagePropertiesForHint(wonNodeInformationService.generateEventURI(localWonNode),
        needURI, Optional.empty(), localWonNode, otherNeedURI, Optional.empty(), originator, score).build();
  }
}
