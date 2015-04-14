import actors.DeadLetterActor;
import actors.MasterCrawlerActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;

import java.io.IOException;

/**
 * User: hfriedrich
 * Date: 27.03.2015
 */
public class AkkaSystemMain
{

  public static void main(String[] args) throws IOException {

    ActorSystem system = ActorSystem.create("AkkaMatchingService");
    system.actorOf(Props.create(MasterCrawlerActor.class), "MasterCrawlerActor");
    ActorRef actor = system.actorOf(Props.create(DeadLetterActor.class), "DeadLetterActor");
    system.eventStream().subscribe(actor, DeadLetter.class);
  }

}
