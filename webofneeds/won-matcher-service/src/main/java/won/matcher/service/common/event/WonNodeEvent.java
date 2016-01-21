package won.matcher.service.common.event;

import java.io.Serializable;

/**
 * This event is used in the matching service to indicate status about won nodes.
 * Usually first a new won node is discovered, then some component can decide to
 * connect to it or skip processing this won node.
 *
 * User: hfriedrich
 * Date: 05.06.2015
 */
public class WonNodeEvent implements Serializable
{
  public enum STATUS
  {
    NEW_WON_NODE_DISCOVERED, CONNECTED_TO_WON_NODE, SKIP_WON_NODE;
  }

  private String wonNodeUri;
  private STATUS status;

  public WonNodeEvent(final String wonNodeUri, STATUS status) {
    this.wonNodeUri = wonNodeUri;
    this.status = status;
  }

  public String getWonNodeUri() {
    return wonNodeUri;
  }

  public STATUS getStatus() {
    return status;
  }

}
