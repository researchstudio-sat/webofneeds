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
package won.bot.framework.eventbot.action.impl.wonmessage.execCommand;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.create.AtomCreationAbortedEvent;
import won.bot.framework.eventbot.event.impl.command.create.CreateAtomCommandEvent;
import won.bot.framework.eventbot.event.impl.command.create.CreateAtomCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.create.CreateAtomCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMATCH;

/**
 * Action executing a CreateAtomCommandEvent, creating the specified atom.
 */
public class ExecuteCreateAtomCommandAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public ExecuteCreateAtomCommandAction(final EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if (!(event instanceof CreateAtomCommandEvent))
            return;
        CreateAtomCommandEvent createAtomCommandEvent = (CreateAtomCommandEvent) event;
        Dataset atomDataset = createAtomCommandEvent.getAtomDataset();
        List<URI> sockets = createAtomCommandEvent.getSockets();
        if (atomDataset == null) {
            logger.warn("CreateAtomCommandEvent did not contain an atom model, aborting atom creation");
            getEventListenerContext().getEventBus().publish(new AtomCreationAbortedEvent(null, null,
                            createAtomCommandEvent,
                            "CreateAtomCommandEvent did not contain an atom model, aborting atom creation"));
            return;
        }
        URI atomUriFromProducer = null;
        Resource atomResource = WonRdfUtils.AtomUtils.getAtomResource(atomDataset);
        if (atomResource.isURIResource()) {
            atomUriFromProducer = URI.create(atomResource.getURI());
            RdfUtils.replaceBaseURI(atomDataset, atomResource.getURI(), true);
        } else {
            RdfUtils.replaceBaseResource(atomDataset, atomResource, true);
        }
        final URI atomUriBeforeCreation = atomUriFromProducer;
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(atomDataset);
        int i = 0;
        for (URI socketURI : sockets) {
            i++;
            atomModelWrapper.addSocket(atomUriBeforeCreation.toString() + "#socket" + i, socketURI.toString());
        }
        final Dataset atomDatasetWithSockets = atomModelWrapper.copyDatasetWithoutSysinfo();
        final URI wonNodeUri = getEventListenerContext().getNodeURISource().getNodeURI();
        logger.debug("creating atom on won node {} with content {} ", wonNodeUri,
                        StringUtils.abbreviate(RdfUtils.toString(atomDatasetWithSockets), 150));
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        final URI atomURI = wonNodeInformationService.generateAtomURI(wonNodeUri);
        RdfUtils.renameResourceWithPrefix(atomDataset, atomResource.getURI(), atomURI.toString());
        WonMessage createAtomMessage = createWonMessage(wonNodeInformationService, atomURI, wonNodeUri,
                        atomDatasetWithSockets, createAtomCommandEvent.isUsedForTesting(),
                        createAtomCommandEvent.isDoNotMatch());
        // remember the atom URI so we can react to success/failure responses
        EventBotActionUtils.rememberInList(getEventListenerContext(), atomURI, createAtomCommandEvent.getUriListName());
        EventListener successCallback = event12 -> {
            logger.debug("atom creation successful, new atom URI is {}", atomURI);
            getEventListenerContext().getEventBus().publish(new CreateAtomCommandSuccessEvent(atomURI,
                            atomUriBeforeCreation, createAtomCommandEvent));
        };
        EventListener failureCallback = event1 -> {
            String textMessage = WonRdfUtils.MessageUtils
                            .getTextMessage(((FailureResponseEvent) event1).getFailureMessage());
            logger.debug("atom creation failed for atom URI {}, original message URI {}: {}", new Object[] {
                            atomURI, ((FailureResponseEvent) event1).getOriginalMessageURI(), textMessage });
            getEventListenerContext().getEventBus().publish(new CreateAtomCommandFailureEvent(atomURI,
                            atomUriBeforeCreation, createAtomCommandEvent, textMessage));
            EventBotActionUtils.removeFromList(getEventListenerContext(), atomURI,
                            createAtomCommandEvent.getUriListName());
        };
        EventBotActionUtils.makeAndSubscribeResponseListener(createAtomMessage, successCallback, failureCallback,
                        getEventListenerContext());
        logger.debug("registered listeners for response to message URI {}", createAtomMessage.getMessageURI());
        getEventListenerContext().getWonMessageSender().sendWonMessage(createAtomMessage);
        logger.debug("atom creation message sent with message URI {}", createAtomMessage.getMessageURI());
    }

    private WonMessage createWonMessage(WonNodeInformationService wonNodeInformationService, URI atomURI,
                    URI wonNodeURI, Dataset atomDataset, final boolean usedForTesting, final boolean doNotMatch)
                    throws WonMessageBuilderException {
        RdfUtils.replaceBaseURI(atomDataset, atomURI.toString(), true);
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(atomDataset);
        if (doNotMatch) {
            atomModelWrapper.addFlag(WONMATCH.NoHintForMe);
            atomModelWrapper.addFlag(WONMATCH.NoHintForCounterpart);
        }
        if (usedForTesting) {
            atomModelWrapper.addFlag(WONMATCH.UsedForTesting);
        }
        return WonMessageBuilder.setMessagePropertiesForCreate(wonNodeInformationService.generateEventURI(wonNodeURI),
                        atomURI, wonNodeURI).addContent(atomModelWrapper.copyDatasetWithoutSysinfo()).build();
    }
}
