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

package won.bot.framework.eventbot.action.impl.wonmessage.execCommand;

import java.net.URI;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.create.CreateNeedCommandEvent;
import won.bot.framework.eventbot.event.impl.command.create.CreateNeedCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.create.CreateNeedCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.command.create.NeedCreationAbortedEvent;
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
public class ExecuteCreateNeedCommandAction extends BaseEventBotAction {

  public ExecuteCreateNeedCommandAction(final EventListenerContext eventListenerContext) {
    super(eventListenerContext);
  }

  @Override
  protected void doRun(Event event, EventListener executingListener) throws Exception {
    if (!(event instanceof CreateNeedCommandEvent))
      return;
    CreateNeedCommandEvent createNeedCommandEvent = (CreateNeedCommandEvent) event;
    Dataset needDataset = createNeedCommandEvent.getNeedDataset();

    List<URI> facets = createNeedCommandEvent.getFacets();
    if (needDataset == null) {
      logger.warn("CreateNeedCommandEvent did not contain a need model, aborting need creation");
      getEventListenerContext().getEventBus().publish(new NeedCreationAbortedEvent(null, null, createNeedCommandEvent,
          "CreateNeedCommandEvent did not contain a need model, aborting need creation"));
      return;
    }

    URI needUriFromProducer = null;
    Resource needResource = WonRdfUtils.NeedUtils.getNeedResource(needDataset);
    if (needResource.isURIResource()) {
      needUriFromProducer = URI.create(needResource.getURI().toString());
      RdfUtils.replaceBaseURI(needDataset, needResource.getURI(), true);
    } else {
      RdfUtils.replaceBaseResource(needDataset, needResource, true);
    }

    final URI needUriBeforeCreation = needUriFromProducer;

    NeedModelWrapper needModelWrapper = new NeedModelWrapper(needDataset);

    int i = 0;
    for (URI facetURI : facets) {
      i++;
      needModelWrapper.addFacet(needUriBeforeCreation.toString() + "#facet" + i, facetURI.toString());
    }
    final Dataset needDatasetWithFacets = needModelWrapper.copyDataset();
    final URI wonNodeUri = getEventListenerContext().getNodeURISource().getNodeURI();
    logger.debug("creating need on won node {} with content {} ", wonNodeUri,
        StringUtils.abbreviate(RdfUtils.toString(needDatasetWithFacets), 150));
    WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
    final URI needURI = wonNodeInformationService.generateNeedURI(wonNodeUri);
    RdfUtils.renameResourceWithPrefix(needDataset, needResource.getURI(), needURI.toString());
    WonMessage createNeedMessage = createWonMessage(wonNodeInformationService, needURI, wonNodeUri,
        needDatasetWithFacets, createNeedCommandEvent.isUsedForTesting(), createNeedCommandEvent.isDoNotMatch());
    // remember the need URI so we can react to success/failure responses
    EventBotActionUtils.rememberInList(getEventListenerContext(), needURI, createNeedCommandEvent.getUriListName());

    EventListener successCallback = new EventListener() {
      @Override
      public void onEvent(Event event) throws Exception {
        logger.debug("need creation successful, new need URI is {}", needURI);
        getEventListenerContext().getEventBus()
            .publish(new CreateNeedCommandSuccessEvent(needURI, needUriBeforeCreation, createNeedCommandEvent));

      }
    };
    EventListener failureCallback = new EventListener() {
      @Override
      public void onEvent(Event event) throws Exception {
        String textMessage = WonRdfUtils.MessageUtils
            .getTextMessage(((FailureResponseEvent) event).getFailureMessage());
        logger.debug("need creation failed for need URI {}, original message URI {}: {}",
            new Object[] { needURI, ((FailureResponseEvent) event).getOriginalMessageURI(), textMessage });
        getEventListenerContext().getEventBus().publish(
            new CreateNeedCommandFailureEvent(needURI, needUriBeforeCreation, createNeedCommandEvent, textMessage));
        EventBotActionUtils.removeFromList(getEventListenerContext(), needURI, createNeedCommandEvent.getUriListName());
      }
    };
    EventBotActionUtils.makeAndSubscribeResponseListener(createNeedMessage, successCallback, failureCallback,
        getEventListenerContext());

    logger.debug("registered listeners for response to message URI {}", createNeedMessage.getMessageURI());
    getEventListenerContext().getWonMessageSender().sendWonMessage(createNeedMessage);
    logger.debug("need creation message sent with message URI {}", createNeedMessage.getMessageURI());

  }

  private WonMessage createWonMessage(WonNodeInformationService wonNodeInformationService, URI needURI, URI wonNodeURI,
      Dataset needDataset, final boolean usedForTesting, final boolean doNotMatch) throws WonMessageBuilderException {

    RdfUtils.replaceBaseURI(needDataset, needURI.toString(), true);

    NeedModelWrapper needModelWrapper = new NeedModelWrapper(needDataset);

    if (doNotMatch) {
      needModelWrapper.addFlag(WON.NO_HINT_FOR_ME);
      needModelWrapper.addFlag(WON.NO_HINT_FOR_COUNTERPART);
    }

    if (usedForTesting) {
      needModelWrapper.addFlag(WON.USED_FOR_TESTING);
    }

    return WonMessageBuilder
        .setMessagePropertiesForCreate(wonNodeInformationService.generateEventURI(wonNodeURI), needURI, wonNodeURI)
        .addContent(needModelWrapper.copyDataset()).build();
  }

}
