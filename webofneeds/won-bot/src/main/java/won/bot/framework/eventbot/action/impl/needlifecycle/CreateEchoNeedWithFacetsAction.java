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

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.commons.lang3.StringUtils;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.NeedCreationFailedEvent;
import won.bot.framework.eventbot.event.impl.matcher.NeedCreatedEventForMatcher;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedCreatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.model.BasicNeedType;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.NeedModelBuilder;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * Creates a need with the specified facets.
 * If no facet is specified, the ownerFacet will be used.
 */
public class CreateEchoNeedWithFacetsAction extends AbstractCreateNeedAction {
    public CreateEchoNeedWithFacetsAction(EventListenerContext eventListenerContext, URI... facets) {
        super(eventListenerContext, facets);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();

        String replyText = "";
        if (!(event instanceof NeedCreatedEventForMatcher)) {
            logger.error("CreateEchoNeedWithFacetsAction can only handle NeedCreatedEventForMatcher");
            return;
        }
        final URI reactingToNeedUri = ((NeedCreatedEventForMatcher) event).getNeedURI();
        final Dataset needDataset = ((NeedCreatedEventForMatcher) event).getNeedData();
        String titleString = WonRdfUtils.NeedUtils.getNeedTitle(needDataset, reactingToNeedUri);
        if (titleString != null) {
            replyText = titleString;
        } else {
            replyText = "Your Posting (" + reactingToNeedUri.toString() + ")";
        }

        WonNodeInformationService wonNodeInformationService = ctx.getWonNodeInformationService();

        final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
        final URI needURI = wonNodeInformationService.generateNeedURI(wonNodeUri);
        final Model needModel =
                new NeedModelBuilder()
                        .setTitle("RE: " + replyText)
                        .setBasicNeedType(BasicNeedType.SUPPLY)
                        .setDescription("This is a need automatically created by the EchoBot.")
                        .setUri(needURI)
                        .setFacetTypes(facets)
                        .build();

        logger.debug("creating need on won node {} with content {} ", wonNodeUri, StringUtils.abbreviate(RdfUtils.toString(needModel), 150));

        WonMessage createNeedMessage = createWonMessage(wonNodeInformationService, needURI, wonNodeUri, needModel);
        //remember the need URI so we can react to success/failure responses
        EventBotActionUtils.rememberInList(ctx, needURI, uriListName);

        EventListener successCallback = new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                logger.debug("need creation successful, new need URI is {}", needURI);

                // save the mapping between the original and the reaction in to the context.
                getEventListenerContext().getBotContextWrapper().addUriAssociation(reactingToNeedUri, needURI);
                ctx.getEventBus().publish(new NeedCreatedEvent(needURI, wonNodeUri, needModel, null));
            }
        };

        EventListener failureCallback = new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                String textMessage = WonRdfUtils.MessageUtils.getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                logger.debug("need creation failed for need URI {}, original message URI {}: {}", new Object[]{needURI, ((FailureResponseEvent) event).getOriginalMessageURI(), textMessage});
                EventBotActionUtils.removeFromList(ctx, needURI, uriListName);
                ctx.getEventBus().publish(new NeedCreationFailedEvent(wonNodeUri));
            }
        };
        EventBotActionUtils.makeAndSubscribeResponseListener(createNeedMessage, successCallback, failureCallback, ctx);

        logger.debug("registered listeners for response to message URI {}", createNeedMessage.getMessageURI());
        getEventListenerContext().getWonMessageSender().sendWonMessage(createNeedMessage);
        logger.debug("need creation message sent with message URI {}", createNeedMessage.getMessageURI());
    }


}
