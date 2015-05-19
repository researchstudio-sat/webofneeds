package node.config;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorContext;
import akka.camel.Camel;
import akka.camel.CamelExtension;
import node.actor.NeedConsumerProtocolActor;
import node.pojo.WonNodeConnection;
import org.apache.activemq.camel.component.ActiveMQComponent;
import won.protocol.service.WonNodeInfo;
import won.protocol.vocabulary.WON;

import java.util.UUID;

/**
 * Factory for creating a {@link node.pojo.WonNodeConnection} with ActiveMq endpoint of a won node
 *
 * User: hfriedrich
 * Date: 18.05.2015
 */
public class ActiveMqNeedConsumerFactory
{
  /**
   * Create a {@link node.pojo.WonNodeConnection} for active mq
   *
   * @param context actor context to create the message consuming actors in
   * @param wonNodeInfo info about the won node (e.g. topics to subscribe)
   * @return
   */
  public static WonNodeConnection createWonNodeConnection(UntypedActorContext context, WonNodeInfo wonNodeInfo) {

    // read won node info
    String activeMq = WON.WON_OVER_ACTIVE_MQ.toString();
    String brokerUri = wonNodeInfo.getSupportedProtocolImplParamValue(
      activeMq, WON.HAS_BROKER_URI.toString());
    String createdTopic = wonNodeInfo.getSupportedProtocolImplParamValue(
      activeMq, WON.HAS_ACTIVEMQ_MATCHER_PROTOCOL_OUT_NEED_CREATED_TOPIC_NAME.toString());
    String activatedTopic = wonNodeInfo.getSupportedProtocolImplParamValue(
      activeMq, WON.HAS_ACTIVEMQ_MATCHER_PROTOCOL_OUT_NEED_ACTIVATED_TOPIC_NAME.toString());
    String deactivatedTopic = wonNodeInfo.getSupportedProtocolImplParamValue(
      activeMq, WON.HAS_ACTIVEMQ_MATCHER_PROTOCOL_OUT_NEED_DEACTIVATED_TOPIC_NAME.toString());

    // init akka camel
    String uuid = UUID.randomUUID().toString();
    String componentName = "activemq-" + uuid;
    Camel camel = CamelExtension.get(context.system());
    camel.context().addComponent(componentName, ActiveMQComponent.activeMQComponent(brokerUri));

    // create the actors that receive the messages
    Props createdProps = Props.create(NeedConsumerProtocolActor.class, componentName + ":topic:" + createdTopic);
    ActorRef created = context.actorOf(createdProps, "ActiveMqNeedCreatedConsumerProtocolActor-" + uuid);

    ActorRef activated = created;
    if (!activatedTopic.equals(createdTopic)) {
      Props activatedProps = Props.create(NeedConsumerProtocolActor.class, componentName + ":topic:" + activatedTopic);
      activated = context.actorOf(activatedProps, "ActiveMqNeedActivatedConsumerProtocolActor-" + uuid);
    }

    ActorRef deactivated;
    if (deactivatedTopic.equals(createdTopic)) {
      deactivated = created;
    } else if (deactivatedTopic.equals(activatedTopic)) {
      deactivated = activated;
    } else {
      Props deactivatedProps = Props.create(NeedConsumerProtocolActor.class, componentName + ":topic:" + deactivatedTopic);
      deactivated = context.actorOf(deactivatedProps, "ActiveMqNeedDeactivatedConsumerProtocolActor-" + uuid);
    }

    return new WonNodeConnection(wonNodeInfo, created, activated, deactivated);
  }
}
