import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import commons.actor.DeadLetterActor;
import crawler.config.CrawlSettings;
import crawler.config.CrawlSettingsImpl;
import crawler.service.CrawlSparqlService;
import node.actor.WonNodeControllerActor;

import java.io.IOException;

/**
 * User: hfriedrich
 * Date: 27.03.2015
 */
public class AkkaSystemCrawlerMain
{

  public static void main(String[] args) throws IOException {

    // setup Akka
    ActorSystem system = ActorSystem.create("AkkaMatchingService");
    CrawlSettingsImpl settings = CrawlSettings.SettingsProvider.get(system);
    CrawlSparqlService endpoint = new CrawlSparqlService(settings.SPARQL_ENDPOINT);
    ActorRef controller = system.actorOf(Props.create(WonNodeControllerActor.class), "WonNodeConrollerActor");
    ActorRef actor = system.actorOf(Props.create(DeadLetterActor.class), "crawler.actor.DeadLetterActor");
    system.eventStream().subscribe(actor, DeadLetter.class);
  }

}
