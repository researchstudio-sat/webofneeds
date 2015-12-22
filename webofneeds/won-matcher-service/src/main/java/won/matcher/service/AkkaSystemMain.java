package won.matcher.service;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import won.matcher.service.common.actor.DeadLetterActor;
import won.matcher.service.common.spring.MatcherServiceAppConfiguration;
import won.matcher.service.common.spring.SpringExtension;
import won.matcher.service.nodemanager.actor.WonNodeControllerActor;
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
