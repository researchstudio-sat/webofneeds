import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.camel.Camel;
import akka.camel.CamelExtension;
import node.actor.NeedConsumerProtocolActor;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.camel.component.jms.JmsComponent;

import java.util.UUID;

/**
 * User: hfriedrich
 * Date: 28.04.2015
 */
public class AkkaSystemControllerMain
{
  public static void main(String[] args) throws Exception {

    ActorSystem system = ActorSystem.create("AkkaMatchingService");
    Camel camel = CamelExtension.get(system);
    String amqUrl = "tcp://localhost:61616";

    ActiveMQConnectionFactory connectionFactory =
      new ActiveMQConnectionFactory("tcp://localhost:61616");
    //connectionFactory.setExceptionListener(new JmsExceptionListener());

    RedeliveryPolicy policy = new RedeliveryPolicy();
    policy.setUseExponentialBackOff(true);
    connectionFactory.setRedeliveryPolicy(policy);



    String uuid = UUID.randomUUID().toString();
    //camel.context().addComponent("activemq", ActiveMQComponent.activeMQComponent(amqUrl));
    camel.context().addComponent("activemq", JmsComponent.jmsComponent(connectionFactory));
    ActorRef actor = system
      .actorOf(Props.create(NeedConsumerProtocolActor.class, "activemq:topic:MatcherProtocol.Out" +
                 ".Need?"),
               "JmsConsumerProtocolActor");

    //camel.context().start();

  }
}
