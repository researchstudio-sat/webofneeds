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
package won.bot.framework.eventbot.action.impl.debugbot;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.counter.Counter;
import won.bot.framework.eventbot.action.impl.counter.CounterImpl;
import won.bot.framework.eventbot.action.impl.atomlifecycle.AbstractCreateAtomAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.AtomCreationFailedEvent;
import won.bot.framework.eventbot.event.AtomSpecificEvent;
import won.bot.framework.eventbot.event.impl.debugbot.ConnectDebugCommandEvent;
import won.bot.framework.eventbot.event.impl.debugbot.HintDebugCommandEvent;
import won.bot.framework.eventbot.event.impl.debugbot.HintType;
import won.bot.framework.eventbot.event.impl.debugbot.AtomCreatedEventForDebugConnect;
import won.bot.framework.eventbot.event.impl.debugbot.AtomCreatedEventForDebugHint;
import won.bot.framework.eventbot.event.impl.matcher.AtomCreatedEventForMatcher;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomCreatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;

/**
 * Creates an atom with the specified sockets. If no socket is specified, the
 * chatSocket will be used.
 */
public class CreateDebugAtomWithSocketsAction extends AbstractCreateAtomAction {
    private Counter counter = new CounterImpl("DebugAtomsCounter");
    private boolean isInitialForHint;
    private boolean isInitialForConnect;

    public CreateDebugAtomWithSocketsAction(final EventListenerContext eventListenerContext,
                    final boolean usedForTesting, final boolean doNotMatch, final URI... sockets) {
        super(eventListenerContext, eventListenerContext.getBotContextWrapper().getAtomCreateListName(), usedForTesting,
                        doNotMatch, sockets);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        String replyText = "";
        URI reactingToAtomUriTmp = null;
        Dataset atomDataset = null;
        if (event instanceof AtomSpecificEvent) {
            reactingToAtomUriTmp = ((AtomSpecificEvent) event).getAtomURI();
        } else {
            logger.warn("could not process non-atom specific event {}", event);
            return;
        }
        if (event instanceof AtomCreatedEventForMatcher) {
            atomDataset = ((AtomCreatedEventForMatcher) event).getAtomData();
        } else if (event instanceof HintDebugCommandEvent) {
            reactingToAtomUriTmp = ((HintDebugCommandEvent) event).getTargetAtomURI();
        } else if (event instanceof ConnectDebugCommandEvent) {
            reactingToAtomUriTmp = ((ConnectDebugCommandEvent) event).getTargetAtomURI();
        } else {
            logger.error("CreateEchoAtomWithSocketsAction cannot handle " + event.getClass().getName());
            return;
        }
        final URI reactingToAtomUri = reactingToAtomUriTmp;
        String titleString = null;
        boolean createAtom = true;
        if (atomDataset != null) {
            DefaultAtomModelWrapper atomModelWrapper = new DefaultAtomModelWrapper(atomDataset);
            titleString = atomModelWrapper.getSomeTitleFromIsOrAll("en", "de");
            createAtom = atomModelWrapper.flag(WON.UsedForTesting) && !atomModelWrapper.flag(WON.NoHintForMe);
        }
        if (!createAtom)
            return; // if create atom is false do not continue the debug atom creation
        if (titleString != null) {
            if (isInitialForConnect) {
                replyText = "Debugging with initial connect: " + titleString;
            } else if (isInitialForHint) {
                replyText = "Debugging with initial hint: " + titleString;
            } else {
                replyText = "Debugging: " + titleString;
            }
        } else {
            replyText = "Debug Atom No. " + counter.increment();
        }
        EventListenerContext ctx = getEventListenerContext();
        WonNodeInformationService wonNodeInformationService = ctx.getWonNodeInformationService();
        EventBus bus = ctx.getEventBus();
        final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
        final URI atomURI = wonNodeInformationService.generateAtomURI(wonNodeUri);
        DefaultAtomModelWrapper atomModelWrapper = new DefaultAtomModelWrapper(atomURI.toString());
        atomModelWrapper.setTitle(replyText);
        atomModelWrapper.setDescription("This is an atom automatically created by the DebugBot.");
        atomModelWrapper.setSeeksTitle(replyText);
        atomModelWrapper.setSeeksDescription("This is an atom automatically created by the DebugBot.");
        int i = 1;
        for (URI socket : sockets) {
            atomModelWrapper.addSocket(atomURI + "#socket" + i, socket.toString());
            i++;
        }
        final Dataset debugAtomDataset = atomModelWrapper.copyDataset();
        final Event origEvent = event;
        logger.debug("creating atom on won node {} with content {} ", wonNodeUri,
                        StringUtils.abbreviate(RdfUtils.toString(debugAtomDataset), 150));
        WonMessage createAtomMessage = createWonMessage(wonNodeInformationService, atomURI, wonNodeUri,
                        debugAtomDataset);
        // remember the atom URI so we can react to success/failure responses
        EventBotActionUtils.rememberInList(ctx, atomURI, uriListName);
        EventListener successCallback = new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                logger.debug("atom creation successful, new atom URI is {}", atomURI);
                // save the mapping between the original and the reaction in to the context.
                getEventListenerContext().getBotContextWrapper().addUriAssociation(reactingToAtomUri, atomURI);
                if (origEvent instanceof HintDebugCommandEvent || isInitialForHint) {
                    HintType hintType = HintType.ATOM_HINT;
                    if (origEvent instanceof HintDebugCommandEvent) {
                        hintType = ((HintDebugCommandEvent) origEvent).getHintType();
                        bus.publish(new AtomCreatedEventForDebugHint(origEvent, atomURI, wonNodeUri,
                                        debugAtomDataset,
                                        hintType));
                    } else {
                        bus.publish(new AtomCreatedEventForDebugHint(origEvent, atomURI, wonNodeUri,
                                        debugAtomDataset,
                                        hintType));
                    }
                } else if ((origEvent instanceof ConnectDebugCommandEvent) || isInitialForConnect) {
                    bus.publish(new AtomCreatedEventForDebugConnect(atomURI, wonNodeUri, debugAtomDataset, null));
                } else {
                    bus.publish(new AtomCreatedEvent(atomURI, wonNodeUri, debugAtomDataset, null));
                }
            }
        };
        EventListener failureCallback = new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                String textMessage = WonRdfUtils.MessageUtils
                                .getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                logger.debug("atom creation failed for atom URI {}, original message URI {}: {}", new Object[] {
                                atomURI, ((FailureResponseEvent) event).getOriginalMessageURI(), textMessage });
                EventBotActionUtils.removeFromList(ctx, atomURI, uriListName);
                bus.publish(new AtomCreationFailedEvent(wonNodeUri));
            }
        };
        EventBotActionUtils.makeAndSubscribeResponseListener(createAtomMessage, successCallback, failureCallback, ctx);
        logger.debug("registered listeners for response to message URI {}", createAtomMessage.getMessageURI());
        ctx.getWonMessageSender().sendWonMessage(createAtomMessage);
        logger.debug("atom creation message sent with message URI {}", createAtomMessage.getMessageURI());
    }

    public void setIsInitialForHint(final boolean isInitialForHint) {
        this.isInitialForHint = isInitialForHint;
    }

    public void setIsInitialForConnect(final boolean isInitialForConnect) {
        this.isInitialForConnect = isInitialForConnect;
    }
}
