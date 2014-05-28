package won.node.facet.businessactivity;

import java.net.URI;

/**
 * User: Danijel
 * Date: 22.5.14.
 */
public interface BAStateManager {
  public URI getStateForNeedUri(URI coordinatorURI, URI participantURI);
  public void setStateForNeedUri(URI stateUri, URI coordinatorURI, URI participantURI);
}