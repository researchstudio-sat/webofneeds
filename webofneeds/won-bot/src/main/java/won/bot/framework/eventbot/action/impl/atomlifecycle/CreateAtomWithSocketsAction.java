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
package won.bot.framework.eventbot.action.impl.atomlifecycle;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.AtomCreationFailedEvent;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomCreatedEvent;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomProducerExhaustedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

/**
 * Creates an atom with the specified sockets. If no socket is specified, the
 * chatSocket will be used.
 */
public class CreateAtomWithSocketsAction extends AbstractCreateAtomAction {
    public CreateAtomWithSocketsAction(EventListenerContext eventListenerContext, String uriListName, URI... sockets) {
        this(eventListenerContext, uriListName, true, false, sockets);
    }

    public CreateAtomWithSocketsAction(final EventListenerContext eventListenerContext, String uriListName,
                    final boolean usedForTesting, final boolean doNotMatch, final URI... sockets) {
        super(eventListenerContext, uriListName, usedForTesting, doNotMatch, sockets);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if (ctx.getAtomProducer().isExhausted()) {
            logger.info("the bot's atom producer is exhausted.");
            ctx.getEventBus().publish(new AtomProducerExhaustedEvent());
            return;
        }
        final Dataset atomDataset = ctx.getAtomProducer().create();
        if (atomDataset == null) {
            logger.warn("atomproducer failed to produce an atom model, aborting atom creation");
            return;
        }
        URI atomUriFromProducer = null;
        Resource atomResource = WonRdfUtils.AtomUtils.getAtomResource(atomDataset);
        if (atomResource.isURIResource()) {
            atomUriFromProducer = URI.create(atomResource.getURI().toString());
            RdfUtils.replaceBaseURI(atomDataset, atomResource.getURI(), true);
        } else {
            RdfUtils.replaceBaseResource(atomDataset, atomResource, true);
        }
        final URI atomUriBeforeCreation = atomUriFromProducer;
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(atomDataset);
        int i = 1;
        for (URI socket : sockets) {
            atomModelWrapper.addSocket(atomUriBeforeCreation.toString() + "#socket" + i, socket.toString());
            i++;
        }
        final Dataset atomDatasetWithSockets = atomModelWrapper.copyDataset();
        final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
        logger.debug("creating atom on won node {} with content {} ", wonNodeUri,
                        StringUtils.abbreviate(RdfUtils.toString(atomDatasetWithSockets), 150));
        WonNodeInformationService wonNodeInformationService = ctx.getWonNodeInformationService();
        final URI atomURI = wonNodeInformationService.generateAtomURI(wonNodeUri);
        WonMessage createAtomMessage = createWonMessage(wonNodeInformationService, atomURI, wonNodeUri,
                        atomDatasetWithSockets);
        // remember the atom URI so we can react to success/failure responses
        EventBotActionUtils.rememberInList(ctx, atomURI, uriListName);
        EventListener successCallback = new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                logger.debug("atom creation successful, new atom URI is {}", atomURI);
                ctx.getEventBus().publish(new AtomCreatedEvent(atomURI, wonNodeUri, atomDatasetWithSockets, null,
                                atomUriBeforeCreation));
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
                ctx.getEventBus().publish(new AtomCreationFailedEvent(wonNodeUri, atomUriBeforeCreation));
            }
        };
        EventBotActionUtils.makeAndSubscribeResponseListener(createAtomMessage, successCallback, failureCallback, ctx);
        logger.debug("registered listeners for response to message URI {}", createAtomMessage.getMessageURI());
        ctx.getWonMessageSender().sendWonMessage(createAtomMessage);
        logger.debug("atom creation message sent with message URI {}", createAtomMessage.getMessageURI());
    }
}
