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
import won.bot.framework.events.action.BaseEventBotAction;
import won.bot.framework.events.action.impl.RegisterMatcherAction;
import won.bot.framework.events.bus.EventBus;
import won.bot.framework.events.event.Event;
import won.bot.framework.events.event.impl.ActEvent;
import won.bot.framework.events.event.impl.NeedCreatedEventForMatcher;
import won.bot.framework.events.listener.BaseEventListener;
import won.bot.framework.events.listener.impl.ActionOnEventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.FacetType;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bot that connects the two last seen needs using a hint.
 */
public class LastSeenNeedsMatcherBot extends EventBot
{

  private BaseEventListener matcherRegistrator;
  private BaseEventListener matcherIndexer;

  private URI matcherUri;

  public void setMatcherUri(final URI matcherUri) {
    this.matcherUri = matcherUri;
  }

  //we remember the need uri each time a new need is encountered
  private AtomicReference<URI> lastNeedUriReference = new AtomicReference<>();
  @Override
  protected void initializeEventListeners()
  {
    EventListenerContext ctx = getEventListenerContext();
    EventBus bus = getEventBus();

    //subscribe this bot with the WoN nodes' 'new need' topic
    this.matcherRegistrator = new ActionOnEventListener(
      ctx,
      new RegisterMatcherAction(ctx),
      1
    );
    bus.subscribe(ActEvent.class,this.matcherRegistrator);

    bus.subscribe(NeedCreatedEventForMatcher.class,
          new ActionOnEventListener(ctx, "lastSeenNeedsMatcher",
            new BaseEventBotAction(ctx)
            {
              @Override
              protected void doRun(final Event event)
                throws Exception {
                NeedCreatedEventForMatcher needCreatedEvent = (NeedCreatedEventForMatcher) event;
                URI currentNeedURI = needCreatedEvent.getNeedURI();
                URI lastNeedURI = lastNeedUriReference.getAndSet(currentNeedURI);
                URI originator = matcherUri;
                if (lastNeedURI == null){
                  logger.info("First invocation. Remembering {} for matching it later", currentNeedURI);
                  return;
                } else {
                  logger.info("Sending hint for {} and {}", currentNeedURI, lastNeedURI);
                }
                ctx.getMatcherProtocolNeedServiceClient().hint(currentNeedURI, lastNeedURI, 0.5, originator,
                                                               null, createWonMessage(
                    currentNeedURI, lastNeedURI, 0.5, originator));
              }
            }));
  }

  private WonMessage createWonMessage(URI needURI, URI otherNeedURI, double score, URI originator)
    throws WonMessageBuilderException {

    WonNodeInformationService wonNodeInformationService =
      getEventListenerContext().getWonNodeInformationService();

    URI localWonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(
      getEventListenerContext().getLinkedDataSource().getDataForResource(needURI), needURI);

    WonMessageBuilder builder = new WonMessageBuilder();
    return builder
      .setMessagePropertiesForHint(
        wonNodeInformationService.generateEventURI(
          localWonNode),
        needURI,
        FacetType.OwnerFacet.getURI(),
        localWonNode,
        otherNeedURI,
        FacetType.OwnerFacet.getURI(),
        originator,
        score)
      .build();
  }
}
