package node.pojo;

import akka.actor.ActorRef;
import won.protocol.service.WonNodeInfo;

/**
 * Class represents all data needed to connect with a won node and receive need updates
 *
 * User: hfriedrich
 * Date: 18.05.2015
 */
public class WonNodeConnection
{
  private WonNodeInfo wonNodeInfo;
  private ActorRef needCreatedConsumer;
  private ActorRef needActivatedConsumer;
  private ActorRef needDeactivatedConsumer;

  public WonNodeConnection(WonNodeInfo info, ActorRef needCreatedConsumer,
                           ActorRef needActivatedConsumer, ActorRef needDeactivatedConsumer) {

    wonNodeInfo = info;
    this.needCreatedConsumer = needCreatedConsumer;
    this.needActivatedConsumer = needActivatedConsumer;
    this.needDeactivatedConsumer = needDeactivatedConsumer;
  }

  public WonNodeInfo getWonNodeInfo() {
    return wonNodeInfo;
  }

  public ActorRef getNeedCreatedConsumer() {
    return needCreatedConsumer;
  }

  public ActorRef getNeedActivatedConsumer() {
    return needActivatedConsumer;
  }

  public ActorRef getNeedDeactivatedConsumer() {
    return needDeactivatedConsumer;
  }
}
