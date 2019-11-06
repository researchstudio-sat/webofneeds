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

import org.apache.jena.query.Dataset;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.impl.command.MessageCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandNotSentEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.command.feedback.FeedbackCommandEvent;
import won.bot.framework.eventbot.event.impl.command.feedback.FeedbackCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.feedback.FeedbackCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SuccessResponseEvent;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONCON;

/**
 * Action executing a ConnectCommandEvent, connecting to the targetAtom on
 * behalf of the atom.
 */
public class ExecuteFeedbackCommandAction extends ExecuteSendMessageCommandAction<FeedbackCommandEvent> {
    public ExecuteFeedbackCommandAction(final EventListenerContext eventListenerContext) {
        super(eventListenerContext, false);
    }

    @Override
    protected MessageCommandFailureEvent createRemoteNodeFailureEvent(FeedbackCommandEvent originalCommand,
                    WonMessage messageSent, FailureResponseEvent failureResponseEvent) {
        return null;
    }

    @Override
    protected MessageCommandSuccessEvent createRemoteNodeSuccessEvent(FeedbackCommandEvent originalCommand,
                    WonMessage messageSent, SuccessResponseEvent successResponseEvent) {
        return null;
    }

    @Override
    protected MessageCommandFailureEvent createLocalNodeFailureEvent(FeedbackCommandEvent originalCommand,
                    WonMessage messageSent, FailureResponseEvent failureResponseEvent) {
        return new FeedbackCommandFailureEvent(originalCommand, failureResponseEvent.getAtomURI(),
                        failureResponseEvent.getTargetAtomURI(), failureResponseEvent.getConnectionURI()
                                        .orElseThrow(() -> new IllegalArgumentException("ConnectionUri must be set")));
    }

    @Override
    protected MessageCommandSuccessEvent createLocalNodeSuccessEvent(FeedbackCommandEvent originalCommand,
                    WonMessage messageSent, SuccessResponseEvent successResponseEvent) {
        return new FeedbackCommandSuccessEvent(originalCommand, originalCommand.getCon());
    }

    @Override
    protected MessageCommandNotSentEvent<?> createMessageNotSentEvent(FeedbackCommandEvent originalCommand,
                    String message) {
        return new MessageCommandNotSentEvent<>(message, originalCommand);
    }

    protected WonMessage createWonMessage(FeedbackCommandEvent feedbackCommandEvent) throws WonMessageBuilderException {
        URI connectionURI = feedbackCommandEvent.getConnectionURI();
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        Dataset connectionRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(connectionURI);
        URI localAtom = WonRdfUtils.ConnectionUtils.getLocalAtomURIFromConnection(connectionRDF, connectionURI);
        URI wonNode = WonRdfUtils.ConnectionUtils.getWonNodeURIFromConnection(connectionRDF, connectionURI);
        // TODO: make more generic by using the URIs specified in the command.
        return WonMessageBuilder
                        .setMessagePropertiesForHintFeedback(wonNodeInformationService.generateEventURI(wonNode),
                                        connectionURI,
                                        localAtom, wonNode,
                                        URI.create(WONCON.Good.getURI()).equals(feedbackCommandEvent.getValue()))
                        .build();
    }
}
