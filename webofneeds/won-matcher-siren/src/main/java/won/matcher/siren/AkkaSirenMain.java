package won.matcher.siren;

import won.matcher.siren.actor.MatcherPubSubActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import won.matcher.service.common.actor.DeadLetterActor;
import won.matcher.service.common.spring.SpringExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import won.matcher.siren.spring.MatcherSirenAppConfiguration;

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
    ActorRef matcherPubSubActor = system.actorOf(
      SpringExtension.SpringExtProvider.get(system).props(MatcherPubSubActor.class), "MatcherPubSubActor");
    ActorRef actor = system.actorOf(Props.create(DeadLetterActor.class), "DeadLetterActor");
    system.eventStream().subscribe(actor, DeadLetter.class);
  }
}
