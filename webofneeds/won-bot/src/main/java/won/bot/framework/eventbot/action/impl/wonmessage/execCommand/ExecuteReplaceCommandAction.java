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

import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.replace.ReplaceAbortedEvent;
import won.bot.framework.eventbot.event.impl.command.replace.ReplaceCommandEvent;
import won.bot.framework.eventbot.event.impl.command.replace.ReplaceCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.replace.ReplaceCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;

/**
 * Action executing a CreateNeedCommandEvent, creating the specified need.
 */
public class ExecuteReplaceCommandAction extends BaseEventBotAction {
    public ExecuteReplaceCommandAction(final EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if (!(event instanceof ReplaceCommandEvent))
            return;
        ReplaceCommandEvent replaceCommandEvent = (ReplaceCommandEvent) event;
        Dataset needDataset = replaceCommandEvent.getNeedDataset();
        if (needDataset == null) {
            logger.warn("ReplaceCommandEvent did not contain a need model, aborting need creation");
            getEventListenerContext().getEventBus().publish(new ReplaceAbortedEvent(null, replaceCommandEvent,
                            "CreateNeedCommandEvent did not contain a need model, aborting need creation"));
            return;
        }
        Resource needResource = WonRdfUtils.NeedUtils.getNeedResource(needDataset);
        if (!needResource.isURIResource()) {
            throw new IllegalArgumentException("need resource in dataset is not an URI");
        }
        URI needURI = URI.create(needResource.getURI().toString());
        RdfUtils.replaceBaseURI(needDataset, needResource.getURI(), true);
        RdfUtils.replaceBaseResource(needDataset, needResource, true);
        NeedModelWrapper needModelWrapper = new NeedModelWrapper(needDataset);
        URI wonNodeURI = getEventListenerContext().getWonNodeInformationService().getWonNodeUri(needURI);
        final URI wonNodeUri = getEventListenerContext().getWonNodeInformationService().getWonNodeUri(needURI);
        logger.debug("replacing need content on won node {} with content {} ", wonNodeUri,
                        StringUtils.abbreviate(RdfUtils.toString(needDataset), 150));
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        RdfUtils.renameResourceWithPrefix(needDataset, needResource.getURI(), needURI.toString());
        WonMessage replaceMessage = createWonMessage(wonNodeInformationService, needURI, wonNodeUri, needDataset);
        EventListener successCallback = new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                logger.debug("need content replacement successful for need URI {}", needURI);
                getEventListenerContext().getEventBus()
                                .publish(new ReplaceCommandSuccessEvent(needURI, replaceCommandEvent));
            }
        };
        EventListener failureCallback = new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                String textMessage = WonRdfUtils.MessageUtils
                                .getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                logger.debug("need content replacement failed for need URI {}, original message URI {}: {}",
                                new Object[] { needURI, ((FailureResponseEvent) event).getOriginalMessageURI(),
                                                textMessage });
                getEventListenerContext().getEventBus()
                                .publish(new ReplaceCommandFailureEvent(needURI, replaceCommandEvent, textMessage));
            }
        };
        EventBotActionUtils.makeAndSubscribeResponseListener(replaceMessage, successCallback, failureCallback,
                        getEventListenerContext());
        logger.debug("registered listeners for response to message URI {}", replaceMessage.getMessageURI());
        getEventListenerContext().getWonMessageSender().sendWonMessage(replaceMessage);
        logger.debug("need content replacement message sent with message URI {}", replaceMessage.getMessageURI());
    }

    private WonMessage createWonMessage(WonNodeInformationService wonNodeInformationService, URI needURI,
                    URI wonNodeURI, Dataset needDataset) throws WonMessageBuilderException {
        RdfUtils.replaceBaseURI(needDataset, needURI.toString(), true);
        NeedModelWrapper needModelWrapper = new NeedModelWrapper(needDataset);
        return WonMessageBuilder.setMessagePropertiesForReplace(wonNodeInformationService.generateEventURI(wonNodeURI),
                        needURI, wonNodeURI).addContent(needModelWrapper.copyDataset()).build();
    }
}
