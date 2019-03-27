package won.node.facet.businessactivity.atomic;

import java.net.URI;
import java.util.HashMap;

import won.protocol.model.Connection;

/**
 * User: Danijel Date: 20.3.14.
 */
public class SimpleATBAConnectionStateManager implements ATBAConnectionStateManager {
    private HashMap<String, ATConnectionState> map = new HashMap();

    public HashMap<String, ATConnectionState> getMap() {
        return map;
    }

    @Override
    public ATConnectionState getStateForConnection(Connection con) {
        return map.get(con.getConnectionURI().toString());
    }

    @Override
    public void setStateForConnection(URI stateUri, Connection con) {
        map.put(con.getConnectionURI().toString(), new ATConnectionState(con, stateUri));
    }

    public URI getStateURIForConnection(Connection con) {
        ATConnectionState cs = this.getStateForConnection(con);
        return cs.getOwnerStateUri();
    }
}
