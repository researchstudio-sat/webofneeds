/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
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
import won.bot.framework.eventbot.event.impl.matcher.AtomCreatedEventForMatcher;
import won.bot.framework.eventbot.listener.BaseEventListener;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

/**
 * Bot that connects the two last seen atoms using a hint.
 */
public class LastSeenAtomsMatcherBot extends EventBot {
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

    // we remember the atom uri each time a new atom is encountered
    private AtomicReference<URI> lastAtomUriReference = new AtomicReference<>();

    @Override
    protected void initializeEventListeners() {
        EventListenerContext ctx = getEventListenerContext();
        EventBus bus = getEventBus();
        // subscribe this bot with the WoN nodes' 'new atom' topic
        RegisterMatcherAction registerMatcherAction = new RegisterMatcherAction(ctx);
        this.matcherRegistrator = new ActionOnEventListener(ctx, registerMatcherAction, 1);
        bus.subscribe(ActEvent.class, this.matcherRegistrator);
        RandomDelayedAction delayedRegistration = new RandomDelayedAction(ctx, registrationMatcherRetryInterval,
                        registrationMatcherRetryInterval, 0, registerMatcherAction);
        ActionOnEventListener matcherRetryRegistrator = new ActionOnEventListener(ctx, delayedRegistration);
        bus.subscribe(MatcherRegisterFailedEvent.class, matcherRetryRegistrator);
        bus.subscribe(AtomCreatedEventForMatcher.class,
                        new ActionOnEventListener(ctx, "lastSeenAtomsMatcher", new BaseEventBotAction(ctx) {
                            @Override
                            protected void doRun(final Event event, EventListener executingListener) throws Exception {
                                AtomCreatedEventForMatcher atomCreatedEvent = (AtomCreatedEventForMatcher) event;
                                URI currentAtomURI = atomCreatedEvent.getAtomURI();
                                URI lastAtomURI = lastAtomUriReference.getAndSet(currentAtomURI);
                                URI originator = matcherUri;
                                if (lastAtomURI == null) {
                                    logger.info("First invocation. Remembering {} for matching it later",
                                                    currentAtomURI);
                                    return;
                                } else {
                                    logger.info("Sending hint for {} and {}", currentAtomURI, lastAtomURI);
                                }
                                ctx.getMatcherProtocolAtomServiceClient().hint(currentAtomURI, lastAtomURI, 0.5,
                                                originator, null,
                                                createWonMessage(currentAtomURI, lastAtomURI, 0.5, originator));
                                ctx.getMatcherProtocolAtomServiceClient().hint(lastAtomURI, currentAtomURI, 0.5,
                                                originator, null,
                                                createWonMessage(lastAtomURI, currentAtomURI, 0.5, originator));
                            }
                        }));
    }

    private WonMessage createWonMessage(URI atomURI, URI otherAtomURI, double score, URI originator)
                    throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        URI localWonNode = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(
                        getEventListenerContext().getLinkedDataSource().getDataForResource(atomURI), atomURI);
        return WonMessageBuilder
                        .setMessagePropertiesForHintToAtom(wonNodeInformationService.generateEventURI(localWonNode),
                                        atomURI, localWonNode, otherAtomURI, originator, score)
                        .build();
    }
}
