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

import org.apache.jena.query.Dataset;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.impl.command.MessageCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandNotSentEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.command.open.OpenCommandEvent;
import won.bot.framework.eventbot.event.impl.command.open.OpenCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.open.OpenCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SuccessResponseEvent;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

/**
 * Action executing a ConnectCommandEvent, connecting to the remoteNeed on behalf of the need.
 */
public class ExecuteOpenCommandAction extends ExecuteSendMessageCommandAction<OpenCommandEvent> {

    public ExecuteOpenCommandAction(final EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected MessageCommandFailureEvent createRemoteNodeFailureEvent(OpenCommandEvent originalCommand,
            WonMessage messageSent, FailureResponseEvent failureResponseEvent) {
        return new OpenCommandFailureEvent(originalCommand, failureResponseEvent.getNeedURI(),
                failureResponseEvent.getRemoteNeedURI(), failureResponseEvent.getConnectionURI());
    }

    @Override
    protected MessageCommandSuccessEvent createRemoteNodeSuccessEvent(OpenCommandEvent originalCommand,
            WonMessage messageSent, SuccessResponseEvent successResponseEvent) {
        return new OpenCommandSuccessEvent(originalCommand, successResponseEvent.getNeedURI(),
                successResponseEvent.getRemoteNeedURI(), successResponseEvent.getConnectionURI());
    }

    @Override
    protected MessageCommandFailureEvent createLocalNodeFailureEvent(OpenCommandEvent originalCommand,
            WonMessage messageSent, FailureResponseEvent failureResponseEvent) {
        return new OpenCommandFailureEvent(originalCommand, failureResponseEvent.getNeedURI(),
                failureResponseEvent.getRemoteNeedURI(), failureResponseEvent.getConnectionURI());
    }

    @Override
    protected MessageCommandSuccessEvent createLocalNodeSuccessEvent(OpenCommandEvent originalCommand,
            WonMessage messageSent, SuccessResponseEvent successResponseEvent) {
        return null;
    }

    @Override
    protected MessageCommandNotSentEvent createMessageNotSentEvent(OpenCommandEvent originalCommand, String message) {
        return new MessageCommandNotSentEvent<OpenCommandEvent>(message, originalCommand);
    }

    protected WonMessage createWonMessage(OpenCommandEvent connectCommandEvent) throws WonMessageBuilderException {
        URI connectionURI = connectCommandEvent.getConnectionURI();
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();

        Dataset connectionRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(connectionURI);
        URI remoteNeed = WonRdfUtils.ConnectionUtils.getRemoteNeedURIFromConnection(connectionRDF, connectionURI);
        URI localNeed = WonRdfUtils.ConnectionUtils.getLocalNeedURIFromConnection(connectionRDF, connectionURI);
        URI wonNode = WonRdfUtils.ConnectionUtils.getWonNodeURIFromConnection(connectionRDF, connectionURI);
        Dataset remoteNeedRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(remoteNeed);

        return WonMessageBuilder.setMessagePropertiesForOpen(wonNodeInformationService.generateEventURI(wonNode),
                connectionURI, localNeed, wonNode,
                WonRdfUtils.ConnectionUtils.getRemoteConnectionURIFromConnection(connectionRDF, connectionURI),
                remoteNeed, WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(remoteNeedRDF, remoteNeed),
                connectCommandEvent.getWelcomeMessage()).build();
    }

}
