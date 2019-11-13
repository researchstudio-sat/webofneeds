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
import won.bot.framework.eventbot.event.impl.command.close.CloseCommandEvent;
import won.bot.framework.eventbot.event.impl.command.close.CloseCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.close.CloseCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SuccessResponseEvent;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

/**
 * Action executing a ConnectCommandEvent, connecting to the targetAtom on
 * behalf of the atom.
 */
public class ExecuteCloseCommandAction extends ExecuteMessageCommandAction<CloseCommandEvent> {
    public ExecuteCloseCommandAction(final EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected MessageCommandFailureEvent createRemoteNodeFailureEvent(CloseCommandEvent originalCommand,
                    WonMessage messageSent, FailureResponseEvent failureResponseEvent) {
        return new CloseCommandFailureEvent(originalCommand, failureResponseEvent.getAtomURI(),
                        failureResponseEvent.getTargetAtomURI(), failureResponseEvent.getConnectionURI()
                                        .orElseThrow(() -> new IllegalArgumentException("ConnectionUri must be set")));
    }

    @Override
    protected MessageCommandSuccessEvent createRemoteNodeSuccessEvent(CloseCommandEvent originalCommand,
                    WonMessage messageSent, SuccessResponseEvent successResponseEvent) {
        return new CloseCommandSuccessEvent(originalCommand, successResponseEvent.getAtomURI(),
                        successResponseEvent.getTargetAtomURI(), successResponseEvent.getConnectionURI()
                                        .orElseThrow(() -> new IllegalArgumentException("ConnectionUri must be set")));
    }

    @Override
    protected MessageCommandFailureEvent createLocalNodeFailureEvent(CloseCommandEvent originalCommand,
                    WonMessage messageSent, FailureResponseEvent failureResponseEvent) {
        return new CloseCommandFailureEvent(originalCommand, failureResponseEvent.getAtomURI(),
                        failureResponseEvent.getTargetAtomURI(), failureResponseEvent.getConnectionURI()
                                        .orElseThrow(() -> new IllegalArgumentException("ConnectionUri must be set")));
    }

    @Override
    protected MessageCommandSuccessEvent createLocalNodeSuccessEvent(CloseCommandEvent originalCommand,
                    WonMessage messageSent, SuccessResponseEvent successResponseEvent) {
        return null;
    }

    @Override
    protected MessageCommandNotSentEvent<?> createMessageNotSentEvent(CloseCommandEvent originalCommand,
                    String message) {
        return new MessageCommandNotSentEvent<>(message, originalCommand);
    }

    protected WonMessage createWonMessage(CloseCommandEvent connectCommandEvent) throws WonMessageBuilderException {
        URI connectionURI = connectCommandEvent.getConnectionURI();
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        Dataset connectionRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(connectionURI);
        URI targetAtom = WonRdfUtils.ConnectionUtils.getTargetAtomURIFromConnection(connectionRDF, connectionURI);
        URI localAtom = WonRdfUtils.ConnectionUtils.getLocalAtomURIFromConnection(connectionRDF, connectionURI);
        URI wonNode = WonRdfUtils.ConnectionUtils.getWonNodeURIFromConnection(connectionRDF, connectionURI);
        Dataset targetAtomRDF = getEventListenerContext().getLinkedDataSource().getDataForResource(targetAtom);
        URI socketURI = WonRdfUtils.ConnectionUtils.getSocketURIFromConnection(connectionRDF, connectionURI);
        URI targetSocketURI = WonRdfUtils.ConnectionUtils.getTargetSocketURIFromConnection(connectionRDF,
                        connectionURI);
        return WonMessageBuilder
                        .close(wonNodeInformationService.generateEventURI(wonNode))
                        .sockets().sender(socketURI).recipient(targetSocketURI)
                        .content().text(connectCommandEvent.getCloseMessage())
                        .build();
    }
}
