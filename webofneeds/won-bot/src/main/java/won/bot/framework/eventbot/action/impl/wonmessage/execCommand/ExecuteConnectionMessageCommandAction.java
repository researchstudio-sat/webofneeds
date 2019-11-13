package won.bot.framework.eventbot.action.impl.wonmessage.execCommand;

import java.net.URI;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.impl.command.MessageCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandNotSentEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SuccessResponseEvent;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.ConnectionMessageBuilder;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

/**
 * Action executing a ConnectionMessageCommandEvent, creating a connection
 * message for sending in the specified connection, adding the specified model
 * as the content of the message.
 */
public class ExecuteConnectionMessageCommandAction
                extends ExecuteMessageCommandAction<ConnectionMessageCommandEvent> {
    public ExecuteConnectionMessageCommandAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext, true);
    }

    @Override
    protected MessageCommandFailureEvent createRemoteNodeFailureEvent(ConnectionMessageCommandEvent originalCommand,
                    WonMessage messageSent, FailureResponseEvent failureResponseEvent) {
        return new ConnectionMessageCommandFailureEvent(originalCommand);
    }

    @Override
    protected MessageCommandSuccessEvent createRemoteNodeSuccessEvent(ConnectionMessageCommandEvent originalCommand,
                    WonMessage messageSent, SuccessResponseEvent successResponseEvent) {
        return new ConnectionMessageCommandSuccessEvent(originalCommand, messageSent);
    }

    @Override
    protected MessageCommandFailureEvent createLocalNodeFailureEvent(ConnectionMessageCommandEvent originalCommand,
                    WonMessage messageSent, FailureResponseEvent failureResponseEvent) {
        return new ConnectionMessageCommandFailureEvent(originalCommand);
    }

    @Override
    protected MessageCommandSuccessEvent createLocalNodeSuccessEvent(ConnectionMessageCommandEvent originalCommand,
                    WonMessage messageSent, SuccessResponseEvent successResponseEvent) {
        return null;
    }

    @Override
    protected MessageCommandNotSentEvent<?> createMessageNotSentEvent(ConnectionMessageCommandEvent originalCommand,
                    String message) {
        return new MessageCommandNotSentEvent<>(message, originalCommand);
    }

    @Override
    protected WonMessage createWonMessage(ConnectionMessageCommandEvent messageCommandEvent)
                    throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        Model localMessageModel = RdfUtils.cloneModel(messageCommandEvent.getMessageModel());
        Dataset connectionRDF = getEventListenerContext().getLinkedDataSource()
                        .getDataForResource(messageCommandEvent.getConnectionURI());
        URI wonNode = WonRdfUtils.ConnectionUtils.getWonNodeURIFromConnection(connectionRDF,
                        messageCommandEvent.getConnectionURI());
        URI messageURI = wonNodeInformationService.generateEventURI(wonNode);
        RdfUtils.replaceBaseURI(localMessageModel, messageURI.toString());
        ConnectionMessageBuilder wmb = WonMessageBuilder.connectionMessage(messageURI)
                        .sockets()
                        /**/.sender(messageCommandEvent.getCon().getSocketURI())
                        /**/.recipient(messageCommandEvent.getCon().getTargetSocketURI())
                        .content().model(localMessageModel);
        Set<URI> injectionTargets = messageCommandEvent.getInjectIntoConnections();
        if (!injectionTargets.isEmpty()) {
            wmb.injectIntoConnections(injectionTargets);
        }
        return wmb.build();
    }
}
