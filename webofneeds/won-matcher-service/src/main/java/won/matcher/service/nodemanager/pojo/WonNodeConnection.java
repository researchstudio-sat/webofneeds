package won.matcher.service.nodemanager.pojo;

import akka.actor.ActorRef;
import won.protocol.service.WonNodeInfo;

/**
 * Class represents all datan needed to connect with a won node, receive atom
 * updates and send hints User: hfriedrich Date: 18.05.2015
 */
public class WonNodeConnection {
    private WonNodeInfo wonNodeInfo;
    private ActorRef atomCreatedConsumer;
    private ActorRef atomActivatedConsumer;
    private ActorRef atomDeactivatedConsumer;
    private ActorRef hintProducer;

    public WonNodeConnection(WonNodeInfo info, ActorRef atomCreatedConsumer, ActorRef atomActivatedConsumer,
                    ActorRef atomDeactivatedConsumer, ActorRef hintProducer) {
        wonNodeInfo = info;
        this.atomCreatedConsumer = atomCreatedConsumer;
        this.atomActivatedConsumer = atomActivatedConsumer;
        this.atomDeactivatedConsumer = atomDeactivatedConsumer;
        this.hintProducer = hintProducer;
    }

    public WonNodeInfo getWonNodeInfo() {
        return wonNodeInfo;
    }

    public ActorRef getAtomCreatedConsumer() {
        return atomCreatedConsumer;
    }

    public ActorRef getAtomActivatedConsumer() {
        return atomActivatedConsumer;
    }

    public ActorRef getAtomDeactivatedConsumer() {
        return atomDeactivatedConsumer;
    }

    public ActorRef getHintProducer() {
        return hintProducer;
    }
}
