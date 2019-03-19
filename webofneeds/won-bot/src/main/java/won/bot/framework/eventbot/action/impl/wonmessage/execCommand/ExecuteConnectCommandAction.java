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
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandSuccessEvent;
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
public class ExecuteConnectCommandAction extends ExecuteSendMessageCommandAction<ConnectCommandEvent> {

    public ExecuteConnectCommandAction(final EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected MessageCommandFailureEvent createRemoteNodeFailureEvent(ConnectCommandEvent originalCommand,
            WonMessage messageSent, FailureResponseEvent failureResponseEvent) {
        return new ConnectCommandFailureEvent(originalCommand, failureResponseEvent.getNeedURI(),
                failureResponseEvent.getRemoteNeedURI(), failureResponseEvent.getConnectionURI());
    }

    @Override
    protected MessageCommandSuccessEvent createRemoteNodeSuccessEvent(ConnectCommandEvent originalCommand,
            WonMessage messageSent, SuccessResponseEvent successResponseEvent) {
        return new ConnectCommandSuccessEvent(originalCommand, successResponseEvent.getNeedURI(),
                successResponseEvent.getRemoteNeedURI(), successResponseEvent.getConnectionURI());
    }

    @Override
    protected MessageCommandFailureEvent createLocalNodeFailureEvent(ConnectCommandEvent originalCommand,
            WonMessage messageSent, FailureResponseEvent failureResponseEvent) {
        return new ConnectCommandFailureEvent(originalCommand, failureResponseEvent.getNeedURI(),
                failureResponseEvent.getRemoteNeedURI(), failureResponseEvent.getConnectionURI());
    }

    @Override
    protected MessageCommandSuccessEvent createLocalNodeSuccessEvent(ConnectCommandEvent originalCommand,
            WonMessage messageSent, SuccessResponseEvent successResponseEvent) {
        return null;
    }

    @Override
    protected MessageCommandNotSentEvent createMessageNotSentEvent(ConnectCommandEvent originalCommand,
            String message) {
        return new MessageCommandNotSentEvent<ConnectCommandEvent>(message, originalCommand);
    }

    protected WonMessage createWonMessage(ConnectCommandEvent connectCommandEvent) throws WonMessageBuilderException {

        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();

        Dataset localNeedRDF = getEventListenerContext().getLinkedDataSource()
                .getDataForResource(connectCommandEvent.getNeedURI());
        Dataset remoteNeedRDF = getEventListenerContext().getLinkedDataSource()
                .getDataForResource(connectCommandEvent.getRemoteNeedURI());

        URI localWonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(localNeedRDF, connectCommandEvent.getNeedURI());
        URI remoteWonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(remoteNeedRDF,
                connectCommandEvent.getRemoteNeedURI());

        return WonMessageBuilder.setMessagePropertiesForConnect(
                wonNodeInformationService.generateEventURI(localWonNode), connectCommandEvent.getLocalFacet(),
                connectCommandEvent.getNeedURI(), localWonNode, connectCommandEvent.getRemoteFacet(),
                connectCommandEvent.getRemoteNeedURI(), remoteWonNode, connectCommandEvent.getWelcomeMessage()).build();
    }

}
