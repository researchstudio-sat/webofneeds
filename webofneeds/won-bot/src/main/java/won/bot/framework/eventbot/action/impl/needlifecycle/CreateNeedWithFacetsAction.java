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

package won.bot.framework.eventbot.action.impl.needlifecycle;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.NeedCreationFailedEvent;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedCreatedEvent;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedProducerExhaustedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * Creates a need with the specified facets.
 * If no facet is specified, the ownerFacet will be used.
 */
public class CreateNeedWithFacetsAction extends AbstractCreateNeedAction {
    public CreateNeedWithFacetsAction(EventListenerContext eventListenerContext, String uriListName, URI... facets) {
        this(eventListenerContext, uriListName, true, false, facets);
    }

    public CreateNeedWithFacetsAction(final EventListenerContext eventListenerContext, String uriListName, final boolean usedForTesting, final boolean doNotMatch, final URI... facets) {
        super(eventListenerContext, uriListName, usedForTesting, doNotMatch, facets);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();

        if (ctx.getNeedProducer().isExhausted()) {
            logger.info("the bot's need producer is exhausted.");
            ctx.getEventBus().publish(new NeedProducerExhaustedEvent());
            return;
        }
        final Model needModel = ctx.getNeedProducer().create();
        if (needModel == null) {
            logger.warn("needproducer failed to produce a need model, aborting need creation");
            return;
        }
        URI needUriFromProducer = null;
        Resource needResource = WonRdfUtils.NeedUtils.getNeedResource(needModel);
        if (needResource.isURIResource()) {
            needUriFromProducer = URI.create(needResource.getURI().toString());
            RdfUtils.replaceBaseURI(needModel, needResource.getURI());
        } else {
            RdfUtils.replaceBaseResource(needModel, needResource);
        }
        final URI needUriBeforeCreation = needUriFromProducer;
        for (URI facetURI : facets) {
            WonRdfUtils.FacetUtils.addFacet(needModel, facetURI);
        }
        final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
        logger.debug("creating need on won node {} with content {} ", wonNodeUri, StringUtils.abbreviate(RdfUtils.toString(needModel), 150));
        WonNodeInformationService wonNodeInformationService =
                ctx.getWonNodeInformationService();
        final URI needURI = wonNodeInformationService.generateNeedURI(wonNodeUri);
        WonMessage createNeedMessage = createWonMessage(wonNodeInformationService,
                needURI, wonNodeUri, needModel);
        //remember the need URI so we can react to success/failure responses
        EventBotActionUtils.rememberInList(ctx, needURI, uriListName);

        EventListener successCallback = new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                logger.debug("need creation successful, new need URI is {}", needURI);
                ctx.getEventBus().publish(new NeedCreatedEvent(needURI, wonNodeUri, needModel, null, needUriBeforeCreation));
            }
        };

        EventListener failureCallback = new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                String textMessage = WonRdfUtils.MessageUtils.getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                logger.debug("need creation failed for need URI {}, original message URI {}: {}", new Object[]{needURI, ((FailureResponseEvent) event).getOriginalMessageURI(), textMessage});
                EventBotActionUtils.removeFromList(ctx, needURI, uriListName);
                ctx.getEventBus().publish(new NeedCreationFailedEvent(wonNodeUri, needUriBeforeCreation));
            }
        };
        EventBotActionUtils.makeAndSubscribeResponseListener(
                createNeedMessage, successCallback, failureCallback, ctx);

        logger.debug("registered listeners for response to message URI {}", createNeedMessage.getMessageURI());
        ctx.getWonMessageSender().sendWonMessage(createNeedMessage);
        logger.debug("need creation message sent with message URI {}", createNeedMessage.getMessageURI());
    }


}
