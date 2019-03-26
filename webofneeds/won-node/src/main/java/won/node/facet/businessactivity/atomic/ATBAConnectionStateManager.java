package won.node.facet.businessactivity.atomic;

import java.net.URI;

import won.protocol.model.Connection;

/**
 * User: Danijel Date: 20.3.14.
 */
public interface ATBAConnectionStateManager {

  public ATConnectionState getStateForConnection(Connection con);

  public void setStateForConnection(URI stateUri, Connection con);
}
