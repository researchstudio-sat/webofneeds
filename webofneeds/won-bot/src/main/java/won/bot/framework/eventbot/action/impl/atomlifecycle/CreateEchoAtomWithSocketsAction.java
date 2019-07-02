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

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.AtomCreationFailedEvent;
import won.bot.framework.eventbot.event.impl.matcher.AtomCreatedEventForMatcher;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomCreatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

/**
 * Creates an atom with the specified sockets. If no socket is specified, the
 * chatSocket will be used.
 */
public class CreateEchoAtomWithSocketsAction extends AbstractCreateAtomAction {
    public CreateEchoAtomWithSocketsAction(EventListenerContext eventListenerContext, URI... sockets) {
        super(eventListenerContext, sockets);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        String replyText = "";
        if (!(event instanceof AtomCreatedEventForMatcher)) {
            logger.error("CreateEchoAtomWithSocketsAction can only handle AtomCreatedEventForMatcher");
            return;
        }
        final URI reactingToAtomUri = ((AtomCreatedEventForMatcher) event).getAtomURI();
        final Dataset atomDataset = ((AtomCreatedEventForMatcher) event).getAtomData();
        DefaultAtomModelWrapper atomModelWrapper = new DefaultAtomModelWrapper(atomDataset);
        String titleString = atomModelWrapper.getSomeTitleFromIsOrAll("en", "de");
        if (titleString != null) {
            replyText = titleString;
        } else {
            replyText = "Your Posting (" + reactingToAtomUri.toString() + ")";
        }
        WonNodeInformationService wonNodeInformationService = ctx.getWonNodeInformationService();
        final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
        final URI atomURI = wonNodeInformationService.generateAtomURI(wonNodeUri);
        atomModelWrapper = new DefaultAtomModelWrapper(atomURI);
        atomModelWrapper.setTitle("RE: " + replyText);
        atomModelWrapper.setDescription("This is an atom automatically created by the EchoBot.");
        atomModelWrapper.setSeeksTitle("RE: " + replyText);
        atomModelWrapper.setSeeksDescription("This is an atom automatically created by the EchoBot.");
        int i = 1;
        for (URI socket : sockets) {
            atomModelWrapper.addSocket(atomURI.toString() + "#socket" + i, socket.toString());
            i++;
        }
        final Dataset echoAtomDataset = atomModelWrapper.copyDataset();
        logger.debug("creating atom on won node {} with content {} ", wonNodeUri,
                        StringUtils.abbreviate(RdfUtils.toString(echoAtomDataset), 150));
        WonMessage createAtomMessage = createWonMessage(wonNodeInformationService, atomURI, wonNodeUri,
                        echoAtomDataset);
        // remember the atom URI so we can react to success/failure responses
        EventBotActionUtils.rememberInList(ctx, atomURI, uriListName);
        EventListener successCallback = new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                logger.debug("atom creation successful, new atom URI is {}", atomURI);
                // save the mapping between the original and the reaction in to the context.
                getEventListenerContext().getBotContextWrapper().addUriAssociation(reactingToAtomUri, atomURI);
                ctx.getEventBus().publish(new AtomCreatedEvent(atomURI, wonNodeUri, echoAtomDataset, null));
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
                ctx.getEventBus().publish(new AtomCreationFailedEvent(wonNodeUri));
            }
        };
        EventBotActionUtils.makeAndSubscribeResponseListener(createAtomMessage, successCallback, failureCallback, ctx);
        logger.debug("registered listeners for response to message URI {}", createAtomMessage.getMessageURI());
        getEventListenerContext().getWonMessageSender().sendWonMessage(createAtomMessage);
        logger.debug("atom creation message sent with message URI {}", createAtomMessage.getMessageURI());
    }
}
