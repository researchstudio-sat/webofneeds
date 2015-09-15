import actor.RescalMatcherActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import common.actor.DeadLetterActor;
import common.spring.SpringExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import spring.MatcherRescalAppConfiguration;

import java.io.IOException;

/**
 * Created by hfriedrich on 15.09.2015.
 */
public class AkkaRescalMain
{
  public static void main(String[] args) throws IOException {

    AnnotationConfigApplicationContext ctx =
      new AnnotationConfigApplicationContext(MatcherRescalAppConfiguration.class);
    ActorSystem system = ctx.getBean(ActorSystem.class);
    ActorRef rescalMatcherActor = system.actorOf(
      SpringExtension.SpringExtProvider.get(system).props(RescalMatcherActor.class), "RescalMatcherActor");
    ActorRef actor = system.actorOf(Props.create(DeadLetterActor.class), "DeadLetterActor");
    system.eventStream().subscribe(actor, DeadLetter.class);
  }
}
