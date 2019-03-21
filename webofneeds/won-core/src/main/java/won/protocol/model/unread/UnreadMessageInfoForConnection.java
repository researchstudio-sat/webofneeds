package won.protocol.model.unread;

import won.protocol.model.ConnectionState;

import java.net.URI;
import java.util.Date;

public class UnreadMessageInfoForConnection {
  private UnreadMessageInfo unreadInformation;
  private URI connectionURI;
  private ConnectionState connectionState;

  public UnreadMessageInfoForConnection(URI connectionURI, ConnectionState connectionState,
      UnreadMessageInfo unreadInformation) {
    super();
    this.connectionURI = connectionURI;
    this.connectionState = connectionState;
    this.unreadInformation = unreadInformation;
  }

  public UnreadMessageInfoForConnection(URI connectionURI, ConnectionState connectionState, long count,
      Date newestTimestamp, Date oldestTimestamp) {
    super();
    this.connectionURI = connectionURI;
    this.connectionState = connectionState;
    this.unreadInformation = new UnreadMessageInfo(count, newestTimestamp, oldestTimestamp);
  }

  public UnreadMessageInfo getUnreadInformation() {
    return unreadInformation;
  }

  public URI getConnectionURI() {
    return connectionURI;
  }

  public ConnectionState getConnectionState() {
    return connectionState;
  }
}
