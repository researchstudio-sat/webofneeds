import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.camel.Camel;
import akka.camel.CamelExtension;
import node.actor.NeedConsumerProtocolActor;
import org.apache.activemq.camel.component.ActiveMQComponent;

import java.io.IOException;
import java.util.UUID;

/**
 * User: hfriedrich
 * Date: 28.04.2015
 */
public class AkkaSystemControllerMain
{
  public static void main(String[] args) throws IOException {

    ActorSystem system = ActorSystem.create("AkkaMatchingService");
    Camel camel = CamelExtension.get(system);
    String amqUrl = "tcp://localhost:61616";
    String uuid = UUID.randomUUID().toString();
    camel.context().addComponent("activemq"+uuid, ActiveMQComponent.activeMQComponent(amqUrl));
    ActorRef actor = system
      .actorOf(Props.create(NeedConsumerProtocolActor.class, "activemq"+ uuid+":topic:MatcherProtocol.Out.Need"),
               "JmsConsumerProtocolActor");
  }
}
