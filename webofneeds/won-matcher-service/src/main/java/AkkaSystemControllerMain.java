import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.camel.Camel;
import akka.camel.CamelExtension;
import org.apache.activemq.camel.component.ActiveMQComponent;
import service.actor.NeedConsumerProtocolActor;

import java.io.IOException;

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
    camel.context().addComponent("activemq", ActiveMQComponent.activeMQComponent(amqUrl));
    ActorRef actor = system.actorOf(Props.create(NeedConsumerProtocolActor.class, "activemq:topic:MatcherProtocol.Out.Need"), "JmsConsumerProtocolActor");
  }
}
