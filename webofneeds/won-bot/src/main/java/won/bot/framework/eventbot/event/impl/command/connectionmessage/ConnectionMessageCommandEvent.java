package won.bot.framework.eventbot.event.impl.command.connectionmessage;

import org.apache.jena.rdf.model.Model;
import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.protocol.message.WonMessageType;
import won.protocol.model.Connection;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ConnectionMessageCommandEvent extends BaseNeedAndConnectionSpecificEvent
    implements MessageCommandEvent, ConnectionSpecificEvent {
  private Model messageModel;
  private Set<URI> injectIntoConnections = new HashSet<>();

  public ConnectionMessageCommandEvent(Connection con, Model messageModel, Collection<URI> injectionTargets) {
    super(con);
    this.messageModel = messageModel;
    if (injectionTargets != null) {
      this.injectIntoConnections.addAll(injectionTargets);
    }
  }

  public ConnectionMessageCommandEvent(Connection con, Model messageModel) {
    this(con, messageModel, null);
  }

  @Override public URI getConnectionURI() {
    return this.getCon().getConnectionURI();
  }

  @Override public WonMessageType getWonMessageType() {
    return WonMessageType.CONNECTION_MESSAGE;
  }

  public Set<URI> getInjectIntoConnections() {
    return injectIntoConnections;
  }

  public Model getMessageModel() {
    return messageModel;
  }
}
