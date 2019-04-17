package won.node.socket.businessactivity.statemanager;

import java.net.URI;

/**
 * User: Danijel Date: 22.5.14.
 */
public interface BAStateManager {
    public URI getStateForAtomUri(URI coordinatorURI, URI participantURI, final URI socketURI);

    public void setStateForAtomUri(URI stateUri, URI statePhaseURI, URI coordinatorURI, URI participantURI,
                    final URI socketURI);

    public void setStateForAtomUri(URI stateUri, URI coordinatorURI, URI participantURI, final URI socketURI);

    public URI getStatePhaseForAtomUri(URI coordinatorURI, URI participantURI, final URI socketURI);
}