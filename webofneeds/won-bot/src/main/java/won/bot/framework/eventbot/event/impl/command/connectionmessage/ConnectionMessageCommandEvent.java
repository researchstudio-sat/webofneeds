package won.bot.framework.eventbot.event.impl.command.connectionmessage;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.rdf.model.Model;

import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.protocol.message.WonMessageType;
import won.protocol.model.Connection;

public class ConnectionMessageCommandEvent extends BaseNeedAndConnectionSpecificEvent implements MessageCommandEvent, ConnectionSpecificEvent {
    private Model messageModel;
    private Set<URI> forwardToReceivers = new HashSet<>();

    public ConnectionMessageCommandEvent(Connection con, Model messageModel, Collection<URI> forwardToReceivers) {
        super(con);
        this.messageModel = messageModel;
        if (forwardToReceivers != null) {
            this.forwardToReceivers.addAll(forwardToReceivers);
        }
    }
    
    public ConnectionMessageCommandEvent(Connection con, Model messageModel) {
        this(con, messageModel, null);
    }

    @Override
    public URI getConnectionURI() {
        return this.getCon().getConnectionURI();
    }

    @Override
    public WonMessageType getWonMessageType() {
        return WonMessageType.CONNECTION_MESSAGE;
    }
    
    public Set<URI> getForwardToReceivers() {
        return forwardToReceivers;
    }

    public Model getMessageModel() {
        return messageModel;
    }
}
