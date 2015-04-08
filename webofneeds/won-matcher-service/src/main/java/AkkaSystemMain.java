import actors.LinkedDataCrawlerActor;
import akka.actor.ActorSystem;
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
    system.actorOf(Props.create(LinkedDataCrawlerActor.class), "LinkedDataCrawler");
  }

}
