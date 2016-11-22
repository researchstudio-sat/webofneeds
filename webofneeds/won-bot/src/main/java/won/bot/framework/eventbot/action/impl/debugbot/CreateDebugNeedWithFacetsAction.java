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

package won.bot.framework.eventbot.action.impl.debugbot;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.sparql.path.PathParser;
import org.apache.commons.lang3.StringUtils;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.needlifecycle.AbstractCreateNeedAction;
import won.bot.framework.eventbot.action.impl.counter.Counter;
import won.bot.framework.eventbot.action.impl.counter.CounterImpl;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.NeedCreationFailedEvent;
import won.bot.framework.eventbot.event.NeedSpecificEvent;
import won.bot.framework.eventbot.event.impl.debugbot.ConnectDebugCommandEvent;
import won.bot.framework.eventbot.event.impl.debugbot.HintDebugCommandEvent;
import won.bot.framework.eventbot.event.impl.debugbot.NeedCreatedEventForDebugConnect;
import won.bot.framework.eventbot.event.impl.debugbot.NeedCreatedEventForDebugHint;
import won.bot.framework.eventbot.event.impl.matcher.NeedCreatedEventForMatcher;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedCreatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.model.BasicNeedType;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DefaultPrefixUtils;
import won.protocol.util.NeedModelBuilder;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * Creates a need with the specified facets.
 * If no facet is specified, the ownerFacet will be used.
 */
public class CreateDebugNeedWithFacetsAction extends AbstractCreateNeedAction
{
    private Counter counter = new CounterImpl("DebugNeedsCounter");

    private boolean isInitialForHint;
    private boolean isInitialForConnect;

    public CreateDebugNeedWithFacetsAction(final EventListenerContext eventListenerContext, final String uriListName,
                                           final boolean usedForTesting, final boolean doNotMatch, final URI... facets) {
        super(eventListenerContext, uriListName, usedForTesting, doNotMatch, facets);
    }

    @Override
    protected void doRun(Event event) throws Exception
    {
        String replyText = "";
        URI reactingToNeedUriTmp = null;
        Dataset needDataset = null;
        if (event instanceof NeedSpecificEvent) {
            reactingToNeedUriTmp = ((NeedSpecificEvent) event).getNeedURI();
        } else {
            logger.warn("could not process non-need specific event {}", event);
            return;
        }
        if (event instanceof NeedCreatedEventForMatcher) {
            needDataset = ((NeedCreatedEventForMatcher) event).getNeedData();
        } else if (event instanceof HintDebugCommandEvent) {
            reactingToNeedUriTmp = ((HintDebugCommandEvent) event).getRemoteNeedURI();
        } else if (event instanceof ConnectDebugCommandEvent) {
            reactingToNeedUriTmp = ((ConnectDebugCommandEvent) event).getRemoteNeedURI();
        } else {
            logger.error("CreateEchoNeedWithFacetsAction cannot handle " + event.getClass().getName());
            return;
        }
        final URI reactingToNeedUri = reactingToNeedUriTmp;
        Path titlePath = PathParser.parse("won:hasContent/dc:title", DefaultPrefixUtils.getDefaultPrefixes());

        String titleString = null;
        boolean createNeed = true;

        if (needDataset != null) {
            titleString = RdfUtils.getStringPropertyForPropertyPath(needDataset, reactingToNeedUri, titlePath);
            createNeed = WonRdfUtils.NeedUtils.hasFlag(needDataset, reactingToNeedUri.toString(), WON.USED_FOR_TESTING)
                         && !WonRdfUtils.NeedUtils.hasFlag(needDataset, reactingToNeedUri.toString(), WON.DO_NOT_MATCH);
        }

        if (!createNeed) return; //if create need is false do not continue the debug need creation

        if (titleString != null){
            if (isInitialForConnect) {
                replyText = "Debugging with initial connect: " + titleString;
            } else if (isInitialForHint) {
                replyText = "Debugging with initial hint: " + titleString;
            } else {
                replyText = "Debugging: " + titleString;
            }
        } else {
            replyText = "Debug Need No. " + counter.increment();
        }

        WonNodeInformationService wonNodeInformationService =
                getEventListenerContext().getWonNodeInformationService();

        final URI wonNodeUri = getEventListenerContext().getNodeURISource().getNodeURI();
        final URI needURI = wonNodeInformationService.generateNeedURI(wonNodeUri);
        final Model needModel =
                new NeedModelBuilder()
                        .setTitle(replyText)
                        .setBasicNeedType(BasicNeedType.DO_TOGETHER)
                        .setDescription("This is a need automatically created by the DebugBot.")
                        .setUri(needURI)
                        .setFacetTypes(facets)
                        .build();

        final Event origEvent = event;

        logger.debug("creating need on won node {} with content {} ", wonNodeUri, StringUtils.abbreviate(RdfUtils.toString(needModel), 150));

        WonMessage createNeedMessage = createWonMessage(wonNodeInformationService,
                needURI, wonNodeUri, needModel);
        //remember the need URI so we can react to success/failure responses
        EventBotActionUtils.rememberInList(getEventListenerContext(), needURI, uriListName);

        EventListener successCallback = new EventListener()
        {
            @Override
            public void onEvent(Event event) throws Exception {
                logger.debug("need creation successful, new need URI is {}", needURI);

                // save the mapping between the original and the reaction in to the context.
                getEventListenerContext().getBotContext().saveToObjectMap(KEY_NEED_REMOTE_NEED_ASSOCIATION,
                                                                          reactingToNeedUri.toString(), needURI);
                getEventListenerContext().getBotContext().saveToObjectMap(KEY_NEED_REMOTE_NEED_ASSOCIATION,
                                                                          needURI.toString(), reactingToNeedUri);

                if ((origEvent instanceof HintDebugCommandEvent) || isInitialForHint) {
                    getEventListenerContext().getEventBus()
                            .publish(new NeedCreatedEventForDebugHint(needURI, wonNodeUri, needModel, null));
                } else if ((origEvent instanceof ConnectDebugCommandEvent) || isInitialForConnect) {
                    getEventListenerContext().getEventBus()
                            .publish(new NeedCreatedEventForDebugConnect(needURI, wonNodeUri, needModel, null));
                } else {
                    getEventListenerContext().getEventBus()
                            .publish(new NeedCreatedEvent(needURI, wonNodeUri, needModel, null));
                }
            }
        };

        EventListener failureCallback = new EventListener()
        {
            @Override
            public void onEvent(Event event) throws Exception {
                String textMessage = WonRdfUtils.MessageUtils.getTextMessage(((FailureResponseEvent) event).getFailureMessage());
                logger.debug("need creation failed for need URI {}, original message URI {}: {}", new Object[]{needURI, ((FailureResponseEvent) event).getOriginalMessageURI(), textMessage});
                EventBotActionUtils.removeFromList(getEventListenerContext(), needURI, uriListName);
                getEventListenerContext().getEventBus().publish(new NeedCreationFailedEvent(wonNodeUri));
            }
        };
        EventBotActionUtils.makeAndSubscribeResponseListener(
                createNeedMessage, successCallback, failureCallback, getEventListenerContext());

        logger.debug("registered listeners for response to message URI {}", createNeedMessage.getMessageURI());
        getEventListenerContext().getWonMessageSender().sendWonMessage(createNeedMessage);
        logger.debug("need creation message sent with message URI {}", createNeedMessage.getMessageURI());
    }


    public void setIsInitialForHint(final boolean isInitialForHint) {
        this.isInitialForHint = isInitialForHint;
    }

    public void setIsInitialForConnect(final boolean isInitialForConnect) {
        this.isInitialForConnect = isInitialForConnect;
    }
}
