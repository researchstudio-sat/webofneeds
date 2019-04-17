package won.node.socket.businessactivity.statemanager;

import java.net.URI;
import java.util.HashMap;

/**
 * User: Danijel Date: 22.5.14.
 */
public abstract class SimpleBAStateManager implements BAStateManager {
    private HashMap<String, URI> map = new HashMap();

    public URI getStateForAtomUri(URI coordinatorURI, URI participantURI, final URI socketURI) {
        return map.get(coordinatorURI.toString() + participantURI.toString() + socketURI.toString());
    }

    public void setStateForAtomUri(URI stateUri, URI coordinatorURI, URI participantURI, final URI socketURI) {
        map.put(coordinatorURI.toString() + participantURI.toString() + socketURI.toString(), stateUri);
    }
}
