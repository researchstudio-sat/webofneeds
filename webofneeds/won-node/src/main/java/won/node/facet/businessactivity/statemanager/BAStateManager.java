package won.node.facet.businessactivity.statemanager;

import java.net.URI;

/**
 * User: Danijel
 * Date: 22.5.14.
 */
public interface BAStateManager {
  public URI getStateForNeedUri(URI coordinatorURI, URI participantURI, final URI facetURI);

  public void setStateForNeedUri(URI stateUri, URI statePhaseURI, URI coordinatorURI, URI participantURI,
      final URI facetURI);

  public void setStateForNeedUri(URI stateUri, URI coordinatorURI, URI participantURI, final URI facetURI);

  public URI getStatePhaseForNeedUri(URI coordinatorURI, URI participantURI, final URI facetURI);
}