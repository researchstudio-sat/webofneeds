import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import common.actor.DeadLetterActor;
import common.spring.MatcherServiceAppConfiguration;
import common.spring.SpringExtension;
import node.actor.WonNodeControllerActor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;

/**
 * User: hfriedrich
 * Date: 27.03.2015
 */
public class AkkaSystemMain
{
  public static void main(String[] args) throws IOException {

    AnnotationConfigApplicationContext ctx =
      new AnnotationConfigApplicationContext(MatcherServiceAppConfiguration.class);
    ActorSystem system = ctx.getBean(ActorSystem.class);
    ActorRef wonNodeControllerActor = system.actorOf(
      SpringExtension.SpringExtProvider.get(system).props(WonNodeControllerActor.class), "WonNodeControllerActor");
    ActorRef actor = system.actorOf(Props.create(DeadLetterActor.class), "DeadLetterActor");
    system.eventStream().subscribe(actor, DeadLetter.class);
  }
}
