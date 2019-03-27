package won.node.facet.businessactivity.statemanager;

import java.net.URI;
import java.util.HashMap;

/**
 * User: Danijel Date: 22.5.14.
 */
public abstract class SimpleBAStateManager implements BAStateManager {
    private HashMap<String, URI> map = new HashMap();

    public URI getStateForNeedUri(URI coordinatorURI, URI participantURI, final URI facetURI) {
        return map.get(coordinatorURI.toString() + participantURI.toString() + facetURI.toString());
    }

    public void setStateForNeedUri(URI stateUri, URI coordinatorURI, URI participantURI, final URI facetURI) {
        map.put(coordinatorURI.toString() + participantURI.toString() + facetURI.toString(), stateUri);
    }
}
