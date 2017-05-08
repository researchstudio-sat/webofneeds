package won.bot.framework.eventbot.event.impl.command.connectionmessage;

import org.apache.jena.rdf.model.Model;
import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.protocol.message.WonMessageType;
import won.protocol.model.Connection;

import java.net.URI;

public class ConnectionMessageCommandEvent extends BaseNeedAndConnectionSpecificEvent implements MessageCommandEvent, ConnectionSpecificEvent {
    private Model messageModel;

    public ConnectionMessageCommandEvent(Connection con, Model messageModel) {
        super(con);
        this.messageModel = messageModel;
    }

    @Override
    public URI getConnectionURI() {
        return this.getCon().getConnectionURI();
    }

    @Override
    public WonMessageType getWonMessageType() {
        return WonMessageType.CONNECTION_MESSAGE;
    }

    public Model getMessageModel() {
        return messageModel;
    }
}
