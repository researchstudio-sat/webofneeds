package won.matcher.service.common.event;

import java.io.Serializable;

import won.protocol.service.WonNodeInfo;

/**
 * This event is used in the matching service to indicate status about won nodes.
 * Usually first a new won node is discovered, then some component can decide to
 * connect to it or skip processing this won node.
 * If we are connected to a won node, we know not only the uri but also the whole won node info.
 *
 * User: hfriedrich
 * Date: 05.06.2015
 */
public class WonNodeEvent implements Serializable
{
  public enum STATUS
  {
    NEW_WON_NODE_DISCOVERED, RETRY_REGISTER_FAILED_WON_NODE, GET_WON_NODE_INFO_FOR_CRAWLING, CONNECTED_TO_WON_NODE, SKIP_WON_NODE, START_CRAWLING_WON_NODE;
  }

  private String wonNodeUri;
  private WonNodeInfo wonNodeInfo;
  private STATUS status;

  public WonNodeEvent(String wonNodeUri, STATUS status) {
    this.wonNodeUri = wonNodeUri;
    this.status = status;
  }

  public WonNodeEvent(String wonNodeUri, STATUS status, WonNodeInfo wonNodeInfo) {
    this.wonNodeUri = wonNodeUri;
    this.status = status;
    this.wonNodeInfo = wonNodeInfo;
  }

  public String getWonNodeUri() {
    return wonNodeUri;
  }

  public WonNodeInfo getWonNodeInfo() {
    return wonNodeInfo;
  }

  public STATUS getStatus() {
    return status;
  }

}
