
import actor.DummyHintReceiverActor;
import actor.SirenMatcherActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import common.actor.DeadLetterActor;
import common.spring.SpringExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import spring.MatcherSirenAppConfiguration;

import java.io.IOException;

/**
 * Created by hfriedrich on 24.08.2015.
 */
public class AkkaSirenMain
{

  public static void main(String[] args) throws IOException {

    AnnotationConfigApplicationContext ctx =
      new AnnotationConfigApplicationContext(MatcherSirenAppConfiguration.class);
    ActorSystem system = ctx.getBean(ActorSystem.class);
    ActorRef wonNodeControllerActor = system.actorOf(
      SpringExtension.SpringExtProvider.get(system).props(SirenMatcherActor.class), "SirenMatcherActor");
    ActorRef actor = system.actorOf(Props.create(DeadLetterActor.class), "DeadLetterActor");
    system.eventStream().subscribe(actor, DeadLetter.class);
  }
}
