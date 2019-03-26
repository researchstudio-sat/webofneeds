package won.matcher.service.nodemanager.pojo;

import akka.actor.ActorRef;
import won.protocol.service.WonNodeInfo;

/**
 * Class represents all data needed to connect with a won node, receive need
 * updates and send hints
 *
 * User: hfriedrich Date: 18.05.2015
 */
public class WonNodeConnection {
  private WonNodeInfo wonNodeInfo;
  private ActorRef needCreatedConsumer;
  private ActorRef needActivatedConsumer;
  private ActorRef needDeactivatedConsumer;
  private ActorRef hintProducer;

  public WonNodeConnection(WonNodeInfo info, ActorRef needCreatedConsumer, ActorRef needActivatedConsumer,
      ActorRef needDeactivatedConsumer, ActorRef hintProducer) {

    wonNodeInfo = info;
    this.needCreatedConsumer = needCreatedConsumer;
    this.needActivatedConsumer = needActivatedConsumer;
    this.needDeactivatedConsumer = needDeactivatedConsumer;
    this.hintProducer = hintProducer;
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

  public ActorRef getHintProducer() {
    return hintProducer;
  }

}
