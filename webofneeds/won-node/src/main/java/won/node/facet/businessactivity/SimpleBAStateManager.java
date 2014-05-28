package won.node.facet.businessactivity;

import java.net.URI;
import java.util.HashMap;

/**
 * User: Danijel
 * Date: 22.5.14.
 */
public class SimpleBAStateManager implements BAStateManager
{
  private HashMap<String, URI> map = new HashMap();

  public URI getStateForNeedUri(URI coordinatorURI, URI participantURI){
    return map.get(coordinatorURI.toString()+ participantURI.toString());
  }

  public void setStateForNeedUri(URI stateUri, URI coordinatorURI, URI participantURI)
  {
    map.put(coordinatorURI.toString()+ participantURI.toString(), stateUri);
  }

}

