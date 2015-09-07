package node.config;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorContext;
import akka.camel.Camel;
import akka.camel.CamelExtension;
import common.spring.SpringExtension;
import node.actor.HintProducerProtocolActor;
import node.actor.NeedConsumerProtocolActor;
import node.pojo.WonNodeConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.FailedToCreateConsumerException;
import org.apache.camel.component.jms.JmsComponent;
import won.protocol.service.WonNodeInfo;
import won.protocol.vocabulary.WON;

import java.util.UUID;

/**
 * Factory for creating a {@link node.pojo.WonNodeConnection} with ActiveMq endpoint of a won node
 *
 * User: hfriedrich
 * Date: 18.05.2015
 */
public class ActiveMqWonNodeConnectionFactory
{
  /**
   * Create a {@link node.pojo.WonNodeConnection} for active mq
   *
   * @param context actor context to create the message consuming actors in
   * @param wonNodeInfo info about the won node (e.g. topics to subscribe)
   * @return the connection
   * @throws FailedToCreateConsumerException
   */
  public static WonNodeConnection createWonNodeConnection(UntypedActorContext context,
                                                          WonNodeInfo wonNodeInfo) {

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
    String hintQueue = wonNodeInfo.getSupportedProtocolImplParamValue(
      activeMq, WON.HAS_ACTIVEMQ_MATCHER_PROTOCOL_QUEUE_NAME.toString());

    // create the activemq component for this won node
    String uuid = UUID.randomUUID().toString();
    String componentName = "activemq-" + uuid;
    ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUri);
    // connectionFactory.setExceptionListener( ... )
    Camel camel = CamelExtension.get(context.system());
    camel.context().addComponent(componentName, JmsComponent.jmsComponent(connectionFactory));

    // create the actors that receive the messages (need events)
    Props createdProps = SpringExtension.SpringExtProvider.get(context.system()).props(
      NeedConsumerProtocolActor.class, componentName + ":topic:" + createdTopic + "?testConnectionOnStartup=false");
    ActorRef created = context.actorOf(createdProps, "ActiveMqNeedCreatedConsumerProtocolActor-" + uuid);

    ActorRef activated = created;
    if (!activatedTopic.equals(createdTopic)) {
      Props activatedProps = SpringExtension.SpringExtProvider.get(context.system()).props(
        NeedConsumerProtocolActor.class, componentName + ":topic:" + activatedTopic + "?testConnectionOnStartup=false");
      activated = context.actorOf(activatedProps, "ActiveMqNeedActivatedConsumerProtocolActor-" + uuid);
    }

    ActorRef deactivated;
    if (deactivatedTopic.equals(createdTopic)) {
      deactivated = created;
    } else if (deactivatedTopic.equals(activatedTopic)) {
      deactivated = activated;
    } else {
      Props deactivatedProps = SpringExtension.SpringExtProvider.get(context.system()).props(
        NeedConsumerProtocolActor.class, componentName + ":topic:" + deactivatedTopic + "?testConnectionOnStartup=false");
      deactivated = context.actorOf(deactivatedProps, "ActiveMqNeedDeactivatedConsumerProtocolActor-" + uuid);
    }

    // create the actor that sends messages (hint events)
    Props hintProps = SpringExtension.SpringExtProvider.get(context.system()).props(
      HintProducerProtocolActor.class, componentName + ":queue:" + hintQueue, brokerUri);
    ActorRef hintProducer = context.actorOf(hintProps, "ActiveMqHintProducerProtocolActor-" + uuid);

    // watch the created consumers from the context to get informed when they are terminated
    context.watch(created);
    context.watch(activated);
    context.watch(deactivated);
    context.watch(hintProducer);

    // create the connection
    WonNodeConnection jmsConnection = new WonNodeConnection(wonNodeInfo, created, activated, deactivated, hintProducer);
    return jmsConnection;
  }
}
